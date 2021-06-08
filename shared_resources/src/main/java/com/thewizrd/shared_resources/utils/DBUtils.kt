package com.thewizrd.shared_resources.utils

import android.content.Context
import android.util.Log
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal object DBUtils {
    suspend fun weatherDataExists(weatherDB: WeatherDatabase): Boolean {
        try {
            val count = weatherDB.weatherDAO().getWeatherDataCount()
            return count > 0
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
            return false
        }
    }

    suspend fun locationDataExists(locationDB: LocationsDatabase): Boolean {
        try {
            locationDB.locationsDAO().getFavoritesCount()
            val count = locationDB.locationsDAO().getLocationDataCount()
            return count > 0
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
            return false
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