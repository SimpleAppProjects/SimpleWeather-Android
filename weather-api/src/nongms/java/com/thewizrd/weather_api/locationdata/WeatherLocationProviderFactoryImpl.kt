package com.thewizrd.weather_api.locationdata

import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.google.location.AndroidLocationProvider
import com.thewizrd.weather_api.google.location.isGeocoderAvailable
import com.thewizrd.weather_api.openweather.citydb.CityDBLocationProvider

class WeatherLocationProviderFactoryImpl : WeatherLocationProviderFactory {
    override fun getLocationProvider(provider: String?): WeatherLocationProvider {
        return when (provider) {
            WeatherAPI.ANDROID -> {
                if (isGeocoderAvailable())
                    AndroidLocationProvider()
                else
                    CityDBLocationProvider()
            }
            //WeatherAPI.ACCUWEATHER -> AccuWeatherLocationProvider()
            WeatherAPI.OPENWEATHERMAP -> CityDBLocationProvider()
            else -> throw IllegalArgumentException("Location provider not supported ($provider)")
        }
    }
}