package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.Pollen

interface PollenProvider {
    @Throws(WeatherException::class)
    suspend fun getPollenData(location: LocationData): Pollen?
}