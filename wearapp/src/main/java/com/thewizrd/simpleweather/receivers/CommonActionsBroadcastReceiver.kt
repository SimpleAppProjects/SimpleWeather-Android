package com.thewizrd.simpleweather.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommonActionsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CommonActionsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            CommonActions.ACTION_SETTINGS_UPDATEAPI,
            CommonActions.ACTION_SETTINGS_UPDATEGPS -> {
                WeatherUpdaterWorker.enqueueAction(
                    context,
                    WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                )
            }
            CommonActions.ACTION_SETTINGS_UPDATEUNIT,
            CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE -> {
                appLib.appScope.launch(Dispatchers.Default) {
                    WidgetUpdaterWorker.requestWidgetUpdate(context)
                }
            }
            CommonActions.ACTION_SETTINGS_UPDATEDATASYNC -> {
                val settingsMgr = SettingsManager(context.applicationContext)
                // Reset UpdateTime value to force a refresh
                settingsMgr.setUpdateTime(DateTimeUtils.LOCALDATETIME_MIN)
            }
        }
        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent?.action)
    }
}