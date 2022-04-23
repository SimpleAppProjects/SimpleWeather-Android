package com.thewizrd.weather_api.tzdb

interface TZDBService {
    suspend fun getTimeZone(latitude: Double, longitude: Double): String?
}