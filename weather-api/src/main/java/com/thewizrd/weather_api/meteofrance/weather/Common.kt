package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Geometry(

    @Json(name = "coordinates")
    val coordinates: List<Float>? = null,

    @Json(name = "type")
    val type: String? = null
)