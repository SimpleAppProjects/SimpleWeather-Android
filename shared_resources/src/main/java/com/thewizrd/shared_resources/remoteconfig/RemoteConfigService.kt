package com.thewizrd.shared_resources.remoteconfig

import com.thewizrd.shared_resources.weatherdata.WeatherAPI

val remoteConfigService: RemoteConfigService by lazy { RemoteConfigServiceImpl() }

interface RemoteConfigService {
    @WeatherAPI.LocationProviders
    fun getLocationProvider(weatherAPI: String): String?
    fun isProviderEnabled(weatherAPI: String): Boolean
    fun updateWeatherProvider(): Boolean

    @WeatherAPI.WeatherProviders
    fun getDefaultWeatherProvider(): String

    @WeatherAPI.WeatherProviders
    fun getDefaultWeatherProvider(countryCode: String?): String

    fun checkConfig()
    suspend fun checkConfigAsync(): Boolean
}