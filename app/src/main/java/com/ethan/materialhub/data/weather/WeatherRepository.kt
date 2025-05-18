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
    private val weatherApi: WeatherApi
) {
    fun getWeatherData(latitude: Double, longitude: Double, apiKey: String): Flow<WeatherResponse> = flow {
        try {
            if (latitude == 0.0 && longitude == 0.0) {
                throw WeatherError.LocationError("Invalid location coordinates")
            }
            
            val response = weatherApi.getWeather(latitude, longitude, apiKey)
            emit(response)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> throw WeatherError.NetworkError("Invalid API key")
                404 -> throw WeatherError.NetworkError("Weather data not found for this location")
                429 -> throw WeatherError.NetworkError("API rate limit exceeded")
                500 -> throw WeatherError.ServerError("Weather service is currently unavailable")
                else -> throw WeatherError.ServerError("Server error: ${e.message()}")
            }
        } catch (e: IOException) {
            throw WeatherError.NetworkError("Network error: Please check your internet connection")
        } catch (e: WeatherError) {
            throw e
        } catch (e: Exception) {
            throw WeatherError.UnknownError("An unexpected error occurred: ${e.message}")
        }
    }
} 