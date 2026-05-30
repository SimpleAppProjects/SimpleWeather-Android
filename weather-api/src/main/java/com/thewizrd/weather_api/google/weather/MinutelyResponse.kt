package com.thewizrd.weather_api.google.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MinutelyResponse(

    @Json(name = "timeZone")
    var timeZone: TimeZone? = null,

    @Json(name = "overallPredictionTimeframe")
    var overallPredictionTimeframe: OverallPredictionTimeframe? = null,

    @Json(name = "segments")
    var segments: List<SegmentsItem>? = null,

    @Json(name = "nextPageToken")
    var nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class OverallPredictionTimeframe(

    @Json(name = "startTime")
    var startTime: String? = null,

    @Json(name = "endTime")
    var endTime: String? = null
)

@JsonClass(generateAdapter = true)
data class SnowfallAmount(

    @Json(name = "unit")
    var unit: String? = null,

    @Json(name = "quantity")
    var quantity: Float? = null
)

@JsonClass(generateAdapter = true)
data class TimeFrame(

    @Json(name = "startTime")
    var startTime: String? = null,

    @Json(name = "endTime")
    var endTime: String? = null
)

@JsonClass(generateAdapter = true)
data class SegmentsItem(

    @Json(name = "intensity")
    var intensity: String? = null,

    @Json(name = "snowfallAmount")
    var snowfallAmount: SnowfallAmount? = null,

    @Json(name = "probability")
    var probability: Int? = null,

    @Json(name = "qpf")
    var qpf: Qpf? = null,

    @Json(name = "type")
    var type: String? = null,

    @Json(name = "timeFrame")
    var timeFrame: TimeFrame? = null
)
