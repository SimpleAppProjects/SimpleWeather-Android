package com.thewizrd.common.migrations

import android.content.Context
import com.thewizrd.shared_resources.database.LocationsDAO
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal object DBUtils {
    fun updateLocationKey(context: Context, locationsDAO: LocationsDAO) {
        GlobalScope.launch(Dispatchers.IO) {
            val settingsMgr = SettingsManager(context)

            for (location in locationsDAO.loadAllLocationData()) {
                val oldKey = location.query
                location.query =
                    weatherModule.weatherManager.getWeatherProvider(location.weatherSource)
                        .updateLocationQuery(location)
                settingsMgr.updateLocationWithKey(location, oldKey)
            }
        }
    }
}