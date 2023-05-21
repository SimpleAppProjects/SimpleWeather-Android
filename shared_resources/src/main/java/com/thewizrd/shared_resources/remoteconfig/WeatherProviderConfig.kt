package com.thewizrd.shared_resources.remoteconfig

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders

@JsonClass(generateAdapter = true)
data class WeatherProviderConfig(
    @Json(name = "locSource")
    @LocationProviders
    var locSource: String? = null,

    @Json(name = "newWeatherSource")
    @WeatherProviders
    var newWeatherSource: String? = null,

    @Json(name = "enabled")
    var isEnabled: Boolean = false
)