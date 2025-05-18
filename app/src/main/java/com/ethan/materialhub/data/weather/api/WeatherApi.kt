package com.ethan.materialhub.data.weather.api

import com.ethan.materialhub.data.weather.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/current.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String, // format: "LAT,LON"
        @Query("aqi") aqi: String = "no"
    ): WeatherResponse
} 