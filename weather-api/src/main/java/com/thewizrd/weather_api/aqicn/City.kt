package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class City(
    @field:Json(name = "geo")
    var geo: List<Double?>? = null,
    @field:Json(name = "name")
    var name: String? = null,
    @field:Json(name = "url")
    var url: String? = null,
)