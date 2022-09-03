package com.thewizrd.shared_resources.locationdata

import android.location.Location
import android.location.LocationManager
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.preferences.SettingsManager

fun buildEmptyGPSLocation(): LocationData {
    var weatherSource: String? = null

    if (SettingsManager.isLoaded()) {
        val settingsMgr = appLib.settingsManager
        weatherSource = settingsMgr.getAPI()
    }

    return LocationQuery.buildEmptyModel(weatherSource)
        .toLocationData(Location(LocationManager.PASSIVE_PROVIDER))
}

fun LocationData.toLocation(): Location {
    return Location(LocationManager.PASSIVE_PROVIDER).let {
        it.latitude = this.latitude
        it.longitude = this.longitude
        it
    }
}