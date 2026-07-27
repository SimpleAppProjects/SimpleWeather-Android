package com.thewizrd.weather_api.openmeteo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AQIResponse(

    @Json(name = "elevation")
    var elevation: Float? = null,

    @Json(name = "generationtime_ms")
    var generationtimeMs: Double? = null,

    @Json(name = "current")
    var current: CurrentAQI? = null,

    @Json(name = "timezone_abbreviation")
    var timezoneAbbreviation: String? = null,

    @Json(name = "current_units")
    var currentUnits: CurrentAQIUnits? = null,

    @Json(name = "timezone")
    var timezone: String? = null,

    @Json(name = "latitude")
    var latitude: Float? = null,

    @Json(name = "utc_offset_seconds")
    var utcOffsetSeconds: Int? = null,

    @Json(name = "longitude")
    var longitude: Float? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAQI(

    @Json(name = "birch_pollen")
    var birchPollen: Float? = null,

    @Json(name = "us_aqi_nitrogen_dioxide")
    var usAqiNitrogenDioxide: Int? = null,

    @Json(name = "us_aqi_pm2_5")
    var usAqiPm25: Int? = null,

    @Json(name = "us_aqi")
    var usAqi: Int? = null,

    @Json(name = "us_aqi_sulphur_dioxide")
    var usAqiSulphurDioxide: Int? = null,

    @Json(name = "us_aqi_ozone")
    var usAqiOzone: Int? = null,

    @Json(name = "us_aqi_pm10")
    var usAqiPm10: Int? = null,

    @Json(name = "grass_pollen")
    var grassPollen: Float? = null,

    @Json(name = "interval")
    var interval: Int? = null,

    @Json(name = "time")
    var time: Long? = null,

    @Json(name = "us_aqi_carbon_monoxide")
    var usAqiCarbonMonoxide: Int? = null,

    @Json(name = "ragweed_pollen")
    var ragweedPollen: Float? = null,

    @Json(name = "alder_pollen")
    var alderPollen: Float? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAQIUnits(

    @Json(name = "birch_pollen")
    var birchPollen: String? = null,

    @Json(name = "us_aqi_nitrogen_dioxide")
    var usAqiNitrogenDioxide: String? = null,

    @Json(name = "us_aqi_pm2_5")
    var usAqiPm25: String? = null,

    @Json(name = "us_aqi")
    var usAqi: String? = null,

    @Json(name = "us_aqi_sulphur_dioxide")
    var usAqiSulphurDioxide: String? = null,

    @Json(name = "us_aqi_ozone")
    var usAqiOzone: String? = null,

    @Json(name = "us_aqi_pm10")
    var usAqiPm10: String? = null,

    @Json(name = "grass_pollen")
    var grassPollen: String? = null,

    @Json(name = "interval")
    var interval: String? = null,

    @Json(name = "time")
    var time: String? = null,

    @Json(name = "us_aqi_carbon_monoxide")
    var usAqiCarbonMonoxide: String? = null,

    @Json(name = "ragweed_pollen")
    var ragweedPollen: String? = null,

    @Json(name = "alder_pollen")
    var alderPollen: String? = null
)
