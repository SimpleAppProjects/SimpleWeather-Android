package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.WeatherException
import java.time.ZonedDateTime

interface AstroDataProviderDateInterface {
    @Throws(WeatherException::class)
    suspend fun getAstronomyData(location: LocationData, date: ZonedDateTime): Astronomy
}