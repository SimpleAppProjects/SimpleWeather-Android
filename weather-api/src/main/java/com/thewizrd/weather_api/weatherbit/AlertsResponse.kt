package com.thewizrd.weather_api.weatherbit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlertsResponse(

    @field:Json(name = "alerts")
    var alerts: List<AlertsItem?>? = null,

    @field:Json(name = "country_code")
    var countryCode: String? = null,

    @field:Json(name = "city_name")
    var cityName: String? = null,

    @field:Json(name = "timezone")
    var timezone: String? = null,

    @field:Json(name = "lon")
    var lon: Double? = null,

    @field:Json(name = "state_code")
    var stateCode: String? = null,

    @field:Json(name = "lat")
    var lat: Double? = null
)

/*
data class AlertsItem(

	@field:Json(name = "severity")
	var severity: String? = null,

	@field:Json(name = "ends_local")
	var endsLocal: String? = null,

	@field:Json(name = "regions")
	var regions: List<String?>? = null,

	@field:Json(name = "expires_local")
	var expiresLocal: String? = null,

	@field:Json(name = "description")
	var description: String? = null,

	@field:Json(name = "onset_utc")
	var onsetUtc: String? = null,

	@field:Json(name = "title")
	var title: String? = null,

	@field:Json(name = "expires_utc")
	var expiresUtc: String? = null,

	@field:Json(name = "uri")
	var uri: String? = null,

	@field:Json(name = "onset_local")
	var onsetLocal: String? = null,

	@field:Json(name = "effective_local")
	var effectiveLocal: String? = null,

	@field:Json(name = "ends_utc")
	var endsUtc: String? = null,

	@field:Json(name = "effective_utc")
	var effectiveUtc: String? = null
)
*/