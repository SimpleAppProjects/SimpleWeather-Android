package com.thewizrd.weather_api.weatherbit

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class AlertsResponse(

    @field:SerializedName("alerts")
    var alerts: List<AlertsItem?>? = null,

    @field:SerializedName("country_code")
    var countryCode: String? = null,

    @field:SerializedName("city_name")
    var cityName: String? = null,

    @field:SerializedName("timezone")
    var timezone: String? = null,

    @field:SerializedName("lon")
    var lon: Double? = null,

    @field:SerializedName("state_code")
    var stateCode: String? = null,

    @field:SerializedName("lat")
    var lat: Double? = null
)

/*
data class AlertsItem(

	@field:SerializedName("severity")
	var severity: String? = null,

	@field:SerializedName("ends_local")
	var endsLocal: String? = null,

	@field:SerializedName("regions")
	var regions: List<String?>? = null,

	@field:SerializedName("expires_local")
	var expiresLocal: String? = null,

	@field:SerializedName("description")
	var description: String? = null,

	@field:SerializedName("onset_utc")
	var onsetUtc: String? = null,

	@field:SerializedName("title")
	var title: String? = null,

	@field:SerializedName("expires_utc")
	var expiresUtc: String? = null,

	@field:SerializedName("uri")
	var uri: String? = null,

	@field:SerializedName("onset_local")
	var onsetLocal: String? = null,

	@field:SerializedName("effective_local")
	var effectiveLocal: String? = null,

	@field:SerializedName("ends_utc")
	var endsUtc: String? = null,

	@field:SerializedName("effective_utc")
	var effectiveUtc: String? = null
)
*/