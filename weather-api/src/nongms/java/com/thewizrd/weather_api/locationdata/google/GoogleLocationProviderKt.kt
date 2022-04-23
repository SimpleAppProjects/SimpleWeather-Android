package com.thewizrd.weather_api.locationdata.google

import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.weather_api.google.location.AndroidLocationProvider

fun getGoogleLocationProvider(): WeatherLocationProvider {
    return AndroidLocationProvider()
}