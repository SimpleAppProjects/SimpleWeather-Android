package com.thewizrd.weather_api.weatherkit

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Availability(
    var datasets: List<String>,
)