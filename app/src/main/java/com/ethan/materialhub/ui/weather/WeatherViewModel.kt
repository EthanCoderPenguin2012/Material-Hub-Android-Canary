package com.ethan.materialhub.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethan.materialhub.data.weather.WeatherRepository
import com.ethan.materialhub.data.weather.model.WeatherResponse
import com.ethan.materialhub.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            weatherRepository.getWeatherData(latitude, longitude, "794d2bd44e894e39bac155724251805")
                .catch { e ->
                    _weatherState.value = WeatherUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { response ->
                    _weatherState.value = WeatherUiState.Success(response)
                }
        }
    }
}

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val weather: WeatherResponse) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
} 