package com.thewizrd.shared_resources.weatherdata

import com.thewizrd.shared_resources.locationdata.LocationData

interface WeatherAlertProviderInterface {
    suspend fun getAlerts(location: LocationData): Collection<WeatherAlert>
}