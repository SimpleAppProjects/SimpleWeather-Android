package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import java.time.ZonedDateTime

interface AstroDataDateProvider {
    @Throws(WeatherException::class)
    suspend fun getAstronomyData(location: LocationData, date: ZonedDateTime): Astronomy
}