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
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    // Use Instant for selected date for better time zone handling
    private val _selectedDate = MutableStateFlow(Instant.now().truncatedTo(ChronoUnit.DAYS))
    val selectedDate: StateFlow<Instant> = _selectedDate

    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState

    private val _defaultCalendarId = MutableStateFlow<Long?>(null)
    val defaultCalendarId: StateFlow<Long?> = _defaultCalendarId

    init {
        viewModelScope.launch {
            // Fetch default calendar ID first
            fetchDefaultCalendarId()

            _selectedDate
                .map { date ->
                    // Calculate start and end time (midnight to midnight) in milliseconds for the selected day
                    val startOfDayMillis = date.toEpochMilli()
                    val endOfDayMillis = date.plus(1, ChronoUnit.DAYS).minusMillis(1).toEpochMilli()
                    Pair(startOfDayMillis, endOfDayMillis)
                }
                .flatMapLatest { (startTime, endTime) ->
                    // Only fetch events if a calendar ID is available and permissions are handled in the repository
                    _defaultCalendarId.filterNotNull().flatMapLatest { calendarRepository.getEvents(startTime, endTime) }
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

    private fun fetchDefaultCalendarId() {
        viewModelScope.launch {
            try {
                _defaultCalendarId.value = calendarRepository.getDefaultCalendarId()
                if (_defaultCalendarId.value == null) {
                    _uiState.value = CalendarUiState.Error("Could not find a default calendar. Please add one on your device.")
                }
            } catch (e: SecurityException) {
                 _uiState.value = CalendarUiState.Error("Calendar permissions are required to find a calendar.")
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Failed to get default calendar ID")
            }
        }
    }


    fun setSelectedDate(date: Date) { // Keep this for now for compatibility with DatePicker which might return Date
        // Convert Date to Instant and truncate to the start of the day
        _selectedDate.value = date.toInstant().truncatedTo(ChronoUnit.DAYS)
    }
    
    fun setSelectedDate(date: Instant) { // Overload for setting date with Instant
        _selectedDate.value = date.truncatedTo(ChronoUnit.DAYS)
    }

    fun addEvent(event: CalendarEvent) {
         if (_defaultCalendarId.value == null) {
             _uiState.value = CalendarUiState.Error("Cannot add event: No calendar selected or found.")
             return
         }

        viewModelScope.launch {
            try {
                // Use the fetched default calendar ID
                calendarRepository.addEvent(event.copy(calendarId = _defaultCalendarId.value!!))
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

     fun setUiStateError(message: String) {
        _uiState.value = CalendarUiState.Error(message)
    }
}

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data object Success : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
} 