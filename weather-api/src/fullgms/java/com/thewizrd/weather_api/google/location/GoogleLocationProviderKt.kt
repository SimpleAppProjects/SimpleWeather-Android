package com.thewizrd.weather_api.google.location

import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.weather_api.locationiq.LocationIQProvider

fun getGoogleLocationProvider(): WeatherLocationProvider {
    return if (isGeocoderAvailable()) {
        GoogleLocationProvider()
    } else {
        LocationIQProvider()
    }
}