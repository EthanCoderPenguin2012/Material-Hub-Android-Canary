package com.ethan.materialhub.data.weather

import com.ethan.materialhub.data.weather.api.WeatherApi
import com.ethan.materialhub.data.weather.model.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    fun getWeatherData(latitude: Double, longitude: Double): Flow<WeatherResponse> = flow {
        try {
            val response = weatherApi.getWeather(latitude, longitude)
            emit(response)
        } catch (e: Exception) {
            // Handle error appropriately
            throw e
        }
    }
} 