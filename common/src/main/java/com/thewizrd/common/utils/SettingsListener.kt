package com.thewizrd.common.utils

import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.NonNull
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.weather_api.weatherModule

// Shared Preferences listener
class SettingsListener(@NonNull private val app: ApplicationLib) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val localBroadcastManager = LocalBroadcastManager.getInstance(app.context)
    private val settingsMgr = SettingsManager(app.context)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key.isNullOrBlank()) return

        val isWeatherLoaded = sharedPreferences.getBoolean(SettingsManager.KEY_WEATHERLOADED, false)

        if (key == SettingsManager.KEY_API) {
            // Weather Provider changed
            weatherModule.weatherManager.updateAPI()
            if (isWeatherLoaded) {
                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI))
            }
        } else if (key == SettingsManager.KEY_USEPERSONALKEY) {
            // Weather Provider changed
            weatherModule.weatherManager.updateAPI()
        } else if (key == SettingsManager.KEY_FOLLOWGPS) {
            if (isWeatherLoaded) {
                val value = sharedPreferences.getBoolean(key, false)
                localBroadcastManager.sendBroadcast(
                    Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS)
                )
                if (app.isPhone) localBroadcastManager.sendBroadcast(
                    Intent(if (value) CommonActions.ACTION_WIDGET_REFRESHWIDGETS else CommonActions.ACTION_WIDGET_RESETWIDGETS)
                )
            }
        } else if (key == SettingsManager.KEY_REFRESHINTERVAL) {
            if (isWeatherLoaded) {
                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEREFRESH))
            }
        } else if (key == SettingsManager.KEY_DATASYNC) {
            if (isWeatherLoaded) {
                // Reset UpdateTime value to force a refresh
                val dataSync = WearableDataSync.valueOf(
                    sharedPreferences.getString(
                        SettingsManager.KEY_DATASYNC,
                        "0"
                    )!!.toInt()
                )
                settingsMgr.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN())
                // Reset interval if setting is off
                if (dataSync == WearableDataSync.OFF) {
                    settingsMgr.setRefreshInterval(SettingsManager.DEFAULT_INTERVAL)
                }
            }
        } else if (key == SettingsManager.KEY_ICONSSOURCE) {
            sharedDeps.weatherIconsManager.updateIconProvider()
        } else if (key == SettingsManager.KEY_DAILYNOTIFICATION) {
            if (isWeatherLoaded) {
                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION))
            }
        } else if (key.startsWith(SettingsManager.KEY_APIKEY_PREFIX)) {
            if (appLib.isPhone) {
                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_SENDUPDATE))
            }
        }
    }
}