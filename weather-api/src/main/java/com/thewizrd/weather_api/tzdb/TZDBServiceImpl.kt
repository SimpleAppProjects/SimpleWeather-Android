package com.thewizrd.weather_api.tzdb

import com.thewizrd.shared_resources.database.TZDatabase
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.tzdb.TZDB
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TZDBServiceImpl : TZDBService {
    override suspend fun getTimeZone(latitude: Double, longitude: Double): String? {
        if (latitude != 0.0 && longitude != 0.0) {
            AnalyticsLogger.logEvent("TZDBCache: querying")
            val tzDB = TZDatabase.getTzdbDAO(sharedDeps.context)

            // Search db if result already exists
            val dbResult = tzDB.getTimeZoneData(latitude, longitude)

            if (!dbResult.isNullOrBlank())
                return dbResult

            // Search tz lookup
            val result = weatherModule.tzProvider.getTimeZone(latitude, longitude)

            if (!result.isNullOrBlank()) {
                // Cache result
                GlobalScope.launch(Dispatchers.IO) {
                    val tzdb = TZDB(latitude, longitude, result)
                    tzDB.insertTZData(tzdb)
                }
            }

            return result
        }

        return "UTC"
    }
}