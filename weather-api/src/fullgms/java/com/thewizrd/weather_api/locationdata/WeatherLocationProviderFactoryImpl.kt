package com.thewizrd.weather_api.locationdata

import android.location.Geocoder
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.google.location.AndroidLocationProvider
import com.thewizrd.weather_api.google.location.GoogleLocationProvider
import com.thewizrd.weather_api.locationiq.LocationIQProvider
import com.thewizrd.weather_api.weatherapi.location.WeatherApiLocationProvider

class WeatherLocationProviderFactoryImpl : WeatherLocationProviderFactory {
    override fun getLocationProvider(provider: String?): WeatherLocationProvider {
        return when (provider) {
            WeatherAPI.ANDROID -> {
                if (Geocoder.isPresent())
                    AndroidLocationProvider()
                else
                    WeatherApiLocationProvider()
            }
            //WeatherAPI.HERE -> HERELocationProvider()
            WeatherAPI.LOCATIONIQ -> LocationIQProvider()
            WeatherAPI.GOOGLE -> GoogleLocationProvider()
            WeatherAPI.WEATHERAPI -> WeatherApiLocationProvider()
            //WeatherAPI.ACCUWEATHER -> AccuWeatherLocationProvider()
            else -> throw IllegalArgumentException("Location provider not supported ($provider)")
        }
    }
}