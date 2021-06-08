package com.thewizrd.shared_resources.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.DBUtils.updateLocationKey
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager

internal object VersionMigrations {
    suspend fun performMigrations(
        context: Context,
        weatherDB: WeatherDatabase,
        locationDB: LocationsDatabase
    ) {
        val settingsMgr = SettingsManager(context.applicationContext)

        val versionCode = try {
            val packageInfo =
                context.applicationContext.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode.toLong()
        } catch (e: Exception) {
            Logger.writeLine(Log.DEBUG, e)
            0
        }

        if (settingsMgr.isWeatherLoaded() && settingsMgr.getVersionCode() < versionCode) {
            // v4.2.0+ (Units)
            if (settingsMgr.getVersionCode() < 294200000) {
                val tempUnit = settingsMgr.getTemperatureUnit()
                if (Units.CELSIUS == tempUnit) {
                    settingsMgr.setDefaultUnits(Units.CELSIUS)
                } else {
                    settingsMgr.setDefaultUnits(Units.FAHRENHEIT)
                }
                if (!SimpleLibrary.instance.app.isPhone) {
                    settingsMgr.setRefreshInterval(SettingsManager.DEFAULTINTERVAL)
                }
            }

            if (settingsMgr.getVersionCode() < 294310000) {
                if (WeatherAPI.HERE == settingsMgr.getAPI()) {
                    // Set default API to Yahoo
                    settingsMgr.setAPI(WeatherAPI.YAHOO)
                    val wm = WeatherManager.instance
                    wm.updateAPI()
                    settingsMgr.setPersonalKey(false)
                    settingsMgr.setKeyVerified(true)
                }
            }

            if (settingsMgr.getVersionCode() < 294320000) {
                // Update location keys
                // NWS key is different now
                if (SimpleLibrary.instance.app.isPhone) {
                    updateLocationKey(context, locationDB)
                }
                settingsMgr.saveLastGPSLocData(LocationData())
            }

            if (settingsMgr.getVersionCode() < 295000000) {
                if (WeatherAPI.YAHOO == settingsMgr.getAPI() || WeatherAPI.HERE == settingsMgr.getAPI()) {
                    // Yahoo Weather API is no longer in service
                    // Set default API to WeatherUnlocked
                    settingsMgr.setAPI(WeatherAPI.WEATHERUNLOCKED)
                    WeatherManager.instance.updateAPI()
                    settingsMgr.setPersonalKey(false)
                    settingsMgr.setKeyVerified(true)
                }
            }

            val bundle = Bundle().apply {
                putString("API", settingsMgr.getAPI())
                putString("API_IsInternalKey", (!settingsMgr.usePersonalKey()).toString())
                putLong("VersionCode", settingsMgr.getVersionCode())
                putLong("CurrentVersionCode", versionCode)
            }
            AnalyticsLogger.logEvent("App_Upgrading", bundle)
        }

        if (versionCode > 0) {
            if (settingsMgr.getVersionCode() < versionCode) {
                FeatureSettings.setUpdateAvailable(false)
            }

            settingsMgr.setVersionCode(versionCode)
        }
    }
}