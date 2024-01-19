package com.thewizrd.shared_resources.remoteconfig

import android.location.Geocoder
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders

class RemoteConfigServiceImpl : RemoteConfigService {
    override fun getLocationProvider(weatherAPI: String): String? {
        return if (Geocoder.isPresent()) {
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
    override fun getDefaultWeatherProvider(location: LocationQuery): String {
        return when {
            LocationUtils.isUS(location) -> {
                WeatherAPI.NWS
            }

            LocationUtils.isFrance(location) -> {
                WeatherAPI.METEOFRANCE
            }

            else -> {
                getDefaultWeatherProvider()
            }
        }
    }

    @WeatherProviders
    override fun getDefaultWeatherProvider(location: LocationData): String {
        return when {
            LocationUtils.isUS(location) -> {
                WeatherAPI.NWS
            }

            LocationUtils.isFrance(location) -> {
                WeatherAPI.METEOFRANCE
            }

            else -> {
                getDefaultWeatherProvider()
            }
        }
    }

    override fun checkConfig() {
        // no-op
    }

    override suspend fun checkConfigAsync(): Boolean {
        return false
    }
}