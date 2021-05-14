package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert

interface WeatherAlertProviderInterface {
    suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>
}