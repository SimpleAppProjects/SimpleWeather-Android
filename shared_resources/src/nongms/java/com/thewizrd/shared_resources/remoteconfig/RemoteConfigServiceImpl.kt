package com.thewizrd.shared_resources.remoteconfig

import android.location.Geocoder
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders

class RemoteConfigServiceImpl : RemoteConfigService {
    override fun getLocationProvider(weatherAPI: String): String? {
        return if (isGeocoderAvailable()) {
            WeatherAPI.ANDROID
        } else {
            WeatherAPI.OPENWEATHERMAP
        }
    }

    override fun isProviderEnabled(weatherAPI: String): Boolean {
        return true
    }

    override fun updateWeatherProvider(): Boolean {
        return false
    }

    @WeatherProviders
    override fun getDefaultWeatherProvider(): String {
        return WeatherAPI.METNO
    }

    @WeatherProviders
    override fun getDefaultWeatherProvider(countryCode: String?): String {
        return if (LocationUtils.isUS(countryCode)) {
            WeatherAPI.NWS
        } else if (LocationUtils.isFrance(countryCode)) {
            WeatherAPI.METEOFRANCE
        } else {
            getDefaultWeatherProvider()
        }
    }

    override fun checkConfig() {
        // no-op
    }

    override suspend fun checkConfigAsync(): Boolean {
        return false
    }
}