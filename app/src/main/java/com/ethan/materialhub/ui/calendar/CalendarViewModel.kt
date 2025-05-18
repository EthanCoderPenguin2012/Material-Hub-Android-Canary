package com.ethan.materialhub.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethan.materialhub.data.calendar.CalendarRepository
import com.ethan.materialhub.data.calendar.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState

    init {
        viewModelScope.launch {
            _selectedDate
                .map { date ->
                    val calendar = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startTime = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val endTime = calendar.timeInMillis
                    Pair(startTime, endTime)
                }
                .flatMapLatest { (startTime, endTime) ->
                    calendarRepository.getEvents(startTime, endTime)
                }
                .catch { e ->
                    _uiState.value = CalendarUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { events ->
                    _events.value = events
                    _uiState.value = CalendarUiState.Success
                }
        }
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
    }

    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                calendarRepository.addEvent(event)
                // Events will be refreshed automatically through the flow
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to add event")
            }
        }
    }

    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                calendarRepository.updateEvent(event)
                // Events will be refreshed automatically through the flow
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to update event")
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                calendarRepository.deleteEvent(eventId)
                // Events will be refreshed automatically through the flow
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to delete event")
            }
        }
    }
}

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data object Success : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
} 