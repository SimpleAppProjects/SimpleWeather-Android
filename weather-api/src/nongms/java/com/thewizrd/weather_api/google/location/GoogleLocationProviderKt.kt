package com.thewizrd.weather_api.google.location

import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider

fun getGoogleLocationProvider(): WeatherLocationProvider {
    return AndroidLocationProvider()
}