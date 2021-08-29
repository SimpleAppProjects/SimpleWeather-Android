package com.thewizrd.shared_resources.utils

import android.content.Context
import android.util.Log
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object DBUtils {
    suspend fun weatherDataExists(weatherDB: WeatherDatabase): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val count = weatherDB.weatherDAO().getWeatherDataCountSync()
                count > 0
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e)
                false
            }
        }
    }

    suspend fun locationDataExists(locationDB: LocationsDatabase): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val count = locationDB.locationsDAO().getLocationDataCountSync()
                count > 0
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e)
                false
            }
        }
    }

    fun setLocationData(locationDB: LocationsDatabase, API: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            for (location in locationDB.locationsDAO().loadAllLocationData()) {
                WeatherManager.getProvider(API).updateLocationData(location)
            }
            locationDB.locationsDAO().loadAllLocationData()
        }
    }

    fun updateLocationKey(context: Context, locationDB: LocationsDatabase) {
        GlobalScope.launch(Dispatchers.IO) {
            val settingsMgr = SettingsManager(context)

            for (location in locationDB.locationsDAO().loadAllLocationData()) {
                val oldKey = location.query
                location.query = WeatherManager.getProvider(location.weatherSource)
                        .updateLocationQuery(location)
                settingsMgr.updateLocationWithKey(location, oldKey)
            }
        }
    }
}