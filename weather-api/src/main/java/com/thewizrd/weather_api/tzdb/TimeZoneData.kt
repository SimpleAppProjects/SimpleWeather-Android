package com.thewizrd.weather_api.tzdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TimeZoneData(
    @Json(name = "tz_long")
    var tzLong: String? = null
)