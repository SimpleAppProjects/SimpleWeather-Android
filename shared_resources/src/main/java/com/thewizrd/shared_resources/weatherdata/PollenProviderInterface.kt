package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.model.Pollen

interface PollenProviderInterface {
    @Throws(WeatherException::class)
    suspend fun getPollenData(location: LocationData): Pollen?
}