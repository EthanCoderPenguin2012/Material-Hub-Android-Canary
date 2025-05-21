package com.ethan.materialhub.data.weather

import com.ethan.materialhub.data.weather.api.WeatherApi
import com.ethan.materialhub.data.weather.model.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class WeatherError : Exception() {
    data class NetworkError(override val message: String) : WeatherError()
    data class LocationError(override val message: String) : WeatherError()
    data class ServerError(override val message: String) : WeatherError()
    data class UnknownError(override val message: String) : WeatherError()
}

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi,
    private val apiKey: String
) {
    fun getWeatherData(latitude: Double, longitude: Double): Flow<WeatherResponse> = flow {
        try {
            if (latitude == 0.0 && longitude == 0.0) {
                throw WeatherError.LocationError("Invalid location coordinates")
            }
            val location = "$latitude,$longitude"
            val response = weatherApi.getWeather(apiKey, location)
            emit(response)
        } catch (e: HttpException) {
            throw WeatherError.ServerError("Server error: ${e.message()}")
        } catch (e: IOException) {
            throw WeatherError.NetworkError("Network error: Please check your internet connection")
        } catch (e: WeatherError) {
            throw e
        } catch (e: Exception) {
            throw WeatherError.UnknownError("An unexpected error occurred: ${e.message}")
        }
    }

    fun getForecastData(latitude: Double, longitude: Double, days: Int = 5): Flow<com.ethan.materialhub.data.weather.model.ForecastResponse> = flow {
        try {
            if (latitude == 0.0 && longitude == 0.0) {
                throw WeatherError.LocationError("Invalid location coordinates")
            }
            val location = "$latitude,$longitude"
            val response = weatherApi.getForecast(apiKey, location, days)
            emit(response)
        } catch (e: HttpException) {
            throw WeatherError.ServerError("Server error: ${e.message()}")
        } catch (e: IOException) {
            throw WeatherError.NetworkError("Network error: Please check your internet connection")
        } catch (e: WeatherError) {
            throw e
        } catch (e: Exception) {
            throw WeatherError.UnknownError("An unexpected error occurred: ${e.message}")
        }
    }
} 