package com.thewizrd.shared_resources.tzdb

interface TimeZoneProviderInterface {
    suspend fun getTimeZone(latitude: Double, longitude: Double): String?
}