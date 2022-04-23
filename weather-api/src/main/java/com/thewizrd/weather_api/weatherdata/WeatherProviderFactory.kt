package com.thewizrd.weather_api.weatherdata

import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProvider

interface WeatherProviderFactory {
    fun getWeatherProvider(@WeatherAPI.WeatherProviders provider: String?): WeatherProvider
}