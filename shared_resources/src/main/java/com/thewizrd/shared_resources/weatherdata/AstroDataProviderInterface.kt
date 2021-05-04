package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.WeatherException

interface AstroDataProviderInterface {
    @Throws(WeatherException::class)
    suspend fun getAstronomyData(location: LocationData): Astronomy
}