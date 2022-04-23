package com.thewizrd.weather_api.locationdata

import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

interface WeatherLocationProviderFactory {
    fun getLocationProvider(@WeatherAPI.LocationProviders provider: String?): WeatherLocationProvider
}