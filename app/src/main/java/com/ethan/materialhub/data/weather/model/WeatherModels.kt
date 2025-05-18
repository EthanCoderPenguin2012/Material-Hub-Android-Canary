package com.ethan.materialhub.data.weather.model

data class WeatherResponse(
    val location: Location,
    val current: Current
)

data class Location(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime: String
)

data class Current(
    val temp_c: Double,
    val condition: Condition,
    val wind_kph: Double,
    val humidity: Int,
    val precip_mm: Double,
    val is_day: Int,
    val uv: Double,
    val sunrise: String?, // Only available in forecast, not current
    val sunset: String?   // Only available in forecast, not current
)

data class Condition(
    val text: String,
    val icon: String
) 