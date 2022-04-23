package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert

interface WeatherAlertProvider {
    suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>?
}