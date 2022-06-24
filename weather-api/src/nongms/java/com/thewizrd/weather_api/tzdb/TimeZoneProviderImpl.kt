package com.thewizrd.weather_api.tzdb

import com.thewizrd.shared_resources.appLib
import com.skedgo.converter.TimezoneMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeZoneProviderImpl : TimeZoneProvider {
    override suspend fun getTimeZone(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.Default) {
            val result = TimezoneMapper.latLngToTimezoneString(latitude, longitude)

            appLib.appScope.launch(Dispatchers.IO) {
                // Run GC since tz lookup takes up a good chunk of memory
                System.gc()
            }

            result
        }
}