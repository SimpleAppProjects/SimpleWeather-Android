package com.thewizrd.weather_api.google.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlertsResponse(

    @Json(name = "regionCode")
    var regionCode: String? = null,

    @Json(name = "weatherAlerts")
    var weatherAlerts: List<PublicAlerts>? = null,

    @Json(name = "nextPageToken")
    var nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class AlertTitle(

    @Json(name = "text")
    var text: String? = null,

    @Json(name = "languageCode")
    var languageCode: String? = null
)

@JsonClass(generateAdapter = true)
data class PublicAlerts(

    @Json(name = "alertId")
    var alertId: String? = null,

    @Json(name = "alertTitle")
    var alertTitle: AlertTitle? = null,

    @Json(name = "eventType")
    var eventType: String? = null,

    @Json(name = "areaName")
    var areaName: String? = null,

    @Json(name = "instruction")
    var instruction: List<String>? = null,

    @Json(name = "safetyRecommendations")
    var safetyRecommendations: List<SafetyRecommendationsItem>? = null,

    @Json(name = "timezoneOffset")
    var timezoneOffset: String? = null,

    @Json(name = "startTime")
    var startTime: String? = null,

    @Json(name = "expirationTime")
    var expirationTime: String? = null,

    @Json(name = "dataSource")
    var dataSource: DataSource? = null,

    @Json(name = "polygon")
    var polygon: String? = null,

    @Json(name = "description")
    var description: String? = null,

    @Json(name = "severity")
    var severity: String? = null,

    @Json(name = "certainty")
    var certainty: String? = null,

    @Json(name = "urgency")
    var urgency: String? = null
)

@JsonClass(generateAdapter = true)
data class DataSource(

    @Json(name = "authorityUri")
    var authorityUri: String? = null,

    @Json(name = "name")
    var name: String? = null,

    @Json(name = "publisher")
    var publisher: String? = null
)

@JsonClass(generateAdapter = true)
data class SafetyRecommendationsItem(

    @Json(name = "directive")
    var directive: String? = null,

    @Json(name = "subtext")
    var subtext: String? = null
)
