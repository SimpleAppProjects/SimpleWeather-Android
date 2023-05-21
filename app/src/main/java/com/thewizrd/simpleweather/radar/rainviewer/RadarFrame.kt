package com.thewizrd.simpleweather.radar.rainviewer

data class RadarFrame(
    val timeStamp: Long,
    val host: String?,
    val path: String?
)