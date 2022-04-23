package com.thewizrd.weather_api.google.location

import android.location.Geocoder
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.weather_api.locationiq.LocationIQProvider

fun getGoogleLocationProvider(): WeatherLocationProvider {
    return if (Geocoder.isPresent()) {
        GoogleLocationProvider()
    } else {
        LocationIQProvider()
    }
}