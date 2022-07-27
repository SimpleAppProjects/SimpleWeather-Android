package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data(
    @Json(name = "iaqi")
    var iaqi: Iaqi? = null,

    @Json(name = "debug")
    var debug: Debug? = null,

    @Json(name = "city")
    var city: City? = null,

    @Json(name = "aqi")
    var aqi: Int? = null,

    @Json(name = "forecast")
    var forecast: Forecast? = null,

    @Json(name = "time")
    var time: Time? = null,

    @Json(name = "idx")
    var idx: Int? = null,

    @Json(name = "attributions")
    var attributions: List<AttributionsItem?>? = null,

    @Json(name = "dominentpol")
    var dominentpol: String? = null,
)