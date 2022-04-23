package com.thewizrd.shared_resources.locationdata

import android.location.Location
import android.location.LocationManager
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.utils.SettingsManager

fun buildEmptyGPSLocation(): LocationData {
    var weatherSource: String? = null

    if (SettingsManager.isLoaded()) {
        val settingsMgr = appLib.settingsManager
        weatherSource = settingsMgr.getAPI()
    }

    return LocationQuery.buildEmptyModel(weatherSource)
        .toLocationData(Location(LocationManager.PASSIVE_PROVIDER))
}