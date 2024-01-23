package com.thewizrd.weather_api.metno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class When(

    @Json(name = "interval")
    val interval: List<String?>? = null
)