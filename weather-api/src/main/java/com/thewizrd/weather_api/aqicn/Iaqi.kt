package com.thewizrd.weather_api.aqicn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Iaqi(
    @Json(name = "no2")
    var no2: No2? = null,

    @Json(name = "p")
    var p: P? = null,

    @Json(name = "o3")
    var o3: O3? = null,

    @Json(name = "pm25")
    var pm25: Pm25? = null,

    @Json(name = "t")
    var t: T? = null,

    @Json(name = "so2")
    var so2: So2? = null,

    @Json(name = "w")
    var w: W? = null,

    @Json(name = "h")
    var h: H? = null,

    @Json(name = "pm10")
    var pm10: Pm10? = null,

    @Json(name = "co")
    var co: Co? = null
)