package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.model.AirQuality

interface AirQualityProviderInterface {
    @Throws(WeatherException::class)
    suspend fun getAirQualityData(location: LocationData): AirQuality?
}