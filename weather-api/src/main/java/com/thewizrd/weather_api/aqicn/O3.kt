package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class O3(
    @Json(name = "v")
    var v: Double? = null
)