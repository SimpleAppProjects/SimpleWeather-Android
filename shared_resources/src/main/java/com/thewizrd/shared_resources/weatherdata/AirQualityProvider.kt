package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.AirQualityData

interface AirQualityProvider {
    @Throws(WeatherException::class)
    suspend fun getAirQualityData(location: LocationData): AirQualityData?
}