package com.thewizrd.weather_api.meteomatics.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherRequest(
    @field:Json(name = "validdates")
    val validdates: String,

    @field:Json(name = "format")
    val format: String,

    @field:Json(name = "location")
    val location: String,

    @field:Json(name = "parameters")
    val parameters: List<String>
)
