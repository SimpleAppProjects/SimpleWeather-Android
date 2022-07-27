package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Daily(
    @Json(name = "o3")
    var o3: List<O3Item?>? = null,

    @Json(name = "pm25")
    var pm25: List<Pm25Item?>? = null,

    @Json(name = "pm10")
    var pm10: List<Pm10Item?>? = null,

    @Json(name = "uvi")
    var uvi: List<UviItem?>? = null,
)