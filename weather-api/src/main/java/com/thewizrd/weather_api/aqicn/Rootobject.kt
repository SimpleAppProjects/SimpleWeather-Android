package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Rootobject(
    @Json(name = "data")
    var data: Data? = null,

    @Json(name = "status")
    var status: String? = null
)