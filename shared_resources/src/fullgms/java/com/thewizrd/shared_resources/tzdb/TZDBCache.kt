package com.thewizrd.shared_resources.tzdb

import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.database.TZDatabase
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TZDBCache {
    suspend fun getTimeZone(latitude: Double, longitude: Double): String? {
        if (latitude != 0.0 && longitude != 0.0) {
            AnalyticsLogger.logEvent("TZDBCache: querying")
            val tzDB = TZDatabase.getInstance(SimpleLibrary.instance.appContext)

            // Search db if result already exists
            val dbResult = tzDB.tzdbDAO().getTimeZoneData(latitude, longitude)

            if (!dbResult.isNullOrBlank())
                return dbResult

            // Search tz lookup
            val result = TimeZoneProvider().getTimeZone(latitude, longitude)

            if (!result.isNullOrBlank()) {
                // Cache result
                GlobalScope.launch(Dispatchers.IO) {
                    val tzdb = TZDB(latitude, longitude, result)
                    tzDB.tzdbDAO().insertTZData(tzdb)
                }
            }

            return result
        }

        return "UTC"
    }
}