package com.thewizrd.shared_resources.remoteconfig

import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders

object RemoteConfig {
    @JvmStatic
    fun getLocationProvider(weatherAPI: String): LocationProviderImpl? {
        return GoogleLocationProvider()
    }

    @JvmStatic
    fun isProviderEnabled(weatherAPI: String): Boolean {
        return true
    }

    @JvmStatic
    fun updateWeatherProvider(): Boolean {
        return false
    }

    @JvmStatic
    @WeatherProviders
    fun getDefaultWeatherProvider(): String {
        return WeatherAPI.METNO
    }

    @JvmStatic
    @WeatherProviders
    fun getDefaultWeatherProvider(countryCode: String?): String {
        return if (LocationUtils.isUS(countryCode)) {
            WeatherAPI.NWS
        } else {
            getDefaultWeatherProvider()
        }
    }

    @JvmStatic
    fun checkConfig() {
        // no-op
    }

    suspend fun checkConfigAsync() {
        // no-op
    }
}