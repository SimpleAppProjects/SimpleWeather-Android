package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Pm25Item(
    @Json(name = "avg")
    var avg: Int? = null,

    @Json(name = "min")
    var min: Int? = null,

    @Json(name = "max")
    var max: Int? = null,

    @Json(name = "day")
    var day: String? = null
)