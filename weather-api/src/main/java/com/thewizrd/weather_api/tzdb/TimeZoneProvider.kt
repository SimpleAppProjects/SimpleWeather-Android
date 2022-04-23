package com.thewizrd.weather_api.tzdb

interface TimeZoneProvider {
    suspend fun getTimeZone(latitude: Double, longitude: Double): String?
}