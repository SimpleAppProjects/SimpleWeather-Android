package com.thewizrd.weather_api.tzdb

import com.skedgo.converter.TimezoneMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeZoneProviderImpl : TimeZoneProvider {
    override suspend fun getTimeZone(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.Default) {
            val result = TimezoneMapper.latLngToTimezoneString(latitude, longitude)

            GlobalScope.launch(Dispatchers.IO) {
                // Run GC since tz lookup takes up a good chunk of memory
                System.gc()
            }

            result
        }
}