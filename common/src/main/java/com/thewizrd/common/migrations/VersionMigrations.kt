package com.thewizrd.common.migrations

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.database.LocationsDAO
import com.thewizrd.shared_resources.database.WeatherDAO
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.preferences.UpdateSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object VersionMigrations {
    suspend fun performMigrations(
        context: Context,
        weatherDAO: WeatherDAO,
        locationsDAO: LocationsDAO
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
                if (!appLib.isPhone) {
                    settingsMgr.setRefreshInterval(SettingsManager.DEFAULT_INTERVAL)
                }
            }

            if (settingsMgr.getVersionCode() < 294310000) {
                if (WeatherAPI.HERE == settingsMgr.getAPI()) {
                    // Set default API to Yahoo
                    settingsMgr.setAPI(WeatherAPI.YAHOO)
                    val wm = weatherModule.weatherManager
                    wm.updateAPI()
                    settingsMgr.setPersonalKey(false)
                    settingsMgr.setKeyVerified(true)
                }
            }

            if (settingsMgr.getVersionCode() < 294320000) {
                // Update location keys
                // NWS key is different now
                if (appLib.isPhone) {
                    DBUtils.updateLocationKey(context, locationsDAO)
                }
                settingsMgr.saveLastGPSLocData(LocationData())
            }

            if (settingsMgr.getVersionCode() < 295000000) {
                if (WeatherAPI.YAHOO == settingsMgr.getAPI() || WeatherAPI.HERE == settingsMgr.getAPI()) {
                    // Yahoo Weather API is no longer in service
                    // Set default API to WeatherUnlocked
                    settingsMgr.setAPI(WeatherAPI.WEATHERUNLOCKED)
                    weatherModule.weatherManager.updateAPI()
                    settingsMgr.setPersonalKey(false)
                    settingsMgr.setKeyVerified(true)
                }
            }

            if (settingsMgr.getVersionCode() < 305300000) {
                // v5.3.0: Clear Glide cache
                // Changed default decode format for Glide
                if (appLib.isPhone) {
                    appLib.appScope.launch(Dispatchers.IO) {
                        com.bumptech.glide.Glide.get(context.applicationContext)
                            .clearDiskCache()
                    }
                }
            }

            if (settingsMgr.getVersionCode() < 315520500) {
                // settings.getAPIKEY -> settings.getAPIKey
                val weatherAPI = settingsMgr.getAPI()
                if (weatherAPI != null) {
                    settingsMgr.setAPIKey(weatherAPI, settingsMgr.getAPIKEY())
                    settingsMgr.setKeyVerified(weatherAPI, settingsMgr.isKeyVerified())
                }

                // DevSettings -> settings.setAPIKey
                val devSettingsMap = settingsMgr.getDevSettingsPreferenceMap()
                devSettingsMap.forEach { (key, value) ->
                    if (value is String) {
                        settingsMgr.setAPIKey(key, value)
                        settingsMgr.setKeyVerified(key, true)
                    }
                }
                settingsMgr.clearDevSettingsPreferences()
            }

            if (settingsMgr.getVersionCode() < 335701300) {
                // Tz refresh
                val locations = mutableListOf<LocationData>()
                if (appLib.isPhone) {
                    locations.addAll(settingsMgr.getLocationData())
                }
                settingsMgr.getLastGPSLocData()?.let { locations.add(it) }

                locations.forEach { location ->
                    if (location.tzLong == "unknown" || location.tzLong == "UTC") {
                        if (location.latitude != 0.0 && location.longitude != 0.0) {
                            val tzId = weatherModule.tzdbService.getTimeZone(
                                location.latitude,
                                location.longitude
                            )
                            if ("unknown" != tzId) {
                                location.tzLong = tzId
                                // Update DB here or somewhere else
                                settingsMgr.updateLocation(location)
                            }
                        }
                    }
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
                UpdateSettings.isUpdateAvailable = false
            }

            settingsMgr.setVersionCode(versionCode)
        }

        if (settingsMgr.getSDKVersionCode() < Build.VERSION.SDK_INT) {
            val start = settingsMgr.getSDKVersionCode()
            val end = Build.VERSION.SDK_INT

            for (i in start..end) {
                when (i) {
                    Build.VERSION_CODES.TIRAMISU -> {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(LocaleUtils.getLocale()))
                    }
                }
            }

            settingsMgr.setSDKVersionCode(end)
        }
    }
}