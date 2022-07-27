package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Time(
    @Json(name = "s")
    var s: String? = null,

    @Json(name = "iso")
    var iso: String? = null,

    @Json(name = "tz")
    var tz: String? = null,

    @Json(name = "v")
    var v: Long? = null
)