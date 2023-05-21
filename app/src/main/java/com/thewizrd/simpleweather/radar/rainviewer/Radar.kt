package com.thewizrd.simpleweather.radar.rainviewer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Radar(
    @Json(name = "past")
    var past: List<RadarItem>? = null,

    @Json(name = "nowcast")
    var nowcast: List<RadarItem>? = null
)