package com.thewizrd.simpleweather.radar.rainviewer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RadarItem(
    @Json(name = "path")
    var path: String? = null,

    @Json(name = "time")
    var time: Int = 0,
)