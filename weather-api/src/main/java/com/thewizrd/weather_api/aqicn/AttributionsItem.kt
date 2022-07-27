package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttributionsItem(
    @Json(name = "name")
    var name: String? = null,

    @Json(name = "url")
    var url: String? = null
)