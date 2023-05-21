package com.thewizrd.simpleweather.radar.rainviewer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherMapsResponse(
    @Json(name = "radar")
    var radar: Radar? = null,

    @Json(name = "generated")
    var generated: Int = 0,

    @Json(name = "host")
    var host: String? = null,

    @Json(name = "version")
    var version: String? = null
)