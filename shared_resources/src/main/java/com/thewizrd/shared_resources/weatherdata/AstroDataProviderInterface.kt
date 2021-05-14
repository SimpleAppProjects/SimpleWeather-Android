package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.model.Astronomy

interface AstroDataProviderInterface {
    @Throws(WeatherException::class)
    suspend fun getAstronomyData(location: LocationData): Astronomy
}