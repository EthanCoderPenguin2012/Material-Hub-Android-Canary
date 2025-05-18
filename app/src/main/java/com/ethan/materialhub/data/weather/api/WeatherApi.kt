package com.ethan.materialhub.data.weather.api

import com.ethan.materialhub.data.weather.model.WeatherResponse
import com.ethan.materialhub.data.weather.model.ForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/current.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String, // format: "LAT,LON"
        @Query("aqi") aqi: String = "no"
    ): WeatherResponse

    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") location: String, // format: "LAT,LON"
        @Query("days") days: Int = 5,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): ForecastResponse
} 