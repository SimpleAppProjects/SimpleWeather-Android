package com.thewizrd.simpleweather.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.services.*
import com.thewizrd.simpleweather.services.UpdaterUtils.Companion.updateAlarm
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WearableWorkerActions
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CommonActionsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CommonActionsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CommonActions.ACTION_SETTINGS_UPDATEAPI -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDSETTINGSUPDATE
                )
                WeatherUpdaterWorker.enqueueAction(
                    context,
                    WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                )
            }
            CommonActions.ACTION_SETTINGS_UPDATEGPS -> {
                WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDUPDATE)
                // Reset notification time for new location
                App.instance.settingsManager.setLastPoPChanceNotificationTime(
                    ZonedDateTime.of(
                        DateTimeUtils.getLocalDateTimeMIN(),
                        ZoneOffset.UTC
                    )
                )
            }
            CommonActions.ACTION_SETTINGS_UPDATEUNIT -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDSETTINGSUPDATE
                )
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            }
            CommonActions.ACTION_SETTINGS_UPDATEREFRESH -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDSETTINGSUPDATE
                )
                updateAlarm(context)
            }
            CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDLOCATIONUPDATE
                )
                if (intent.getBooleanExtra(CommonActions.EXTRA_FORCEUPDATE, true)) {
                    WeatherUpdaterWorker.enqueueAction(
                        context,
                        WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                    )
                }
                // Reset notification time for new location
                App.instance.settingsManager.setLastPoPChanceNotificationTime(
                    ZonedDateTime.of(
                        DateTimeUtils.getLocalDateTimeMIN(),
                        ZoneOffset.UTC
                    )
                )
            }
            CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION -> {
                val oldKey = intent.getStringExtra(Constants.WIDGETKEY_OLDKEY)
                val locationJson = intent.getStringExtra(Constants.WIDGETKEY_LOCATION)

                GlobalScope.launch(Dispatchers.Default) {
                    val location = JSONParser.deserializer(locationJson, LocationData::class.java)

                    if (WidgetUtils.exists(oldKey)) {
                        WidgetUtils.updateWidgetIds(oldKey, location)
                    }
                }
            }
            CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDWEATHERUPDATE
                )
            }
            CommonActions.ACTION_SETTINGS_SENDUPDATE -> {
                WearableWorker.enqueueAction(
                    context,
                    WearableWorkerActions.ACTION_SENDSETTINGSUPDATE
                )
            }
            CommonActions.ACTION_WIDGET_RESETWIDGETS -> {
                WidgetWorker.enqueueResetGPSWidgets(context)
            }
            CommonActions.ACTION_WIDGET_REFRESHWIDGETS -> {
                WidgetWorker.enqueueRefreshGPSWidgets(context)
            }
            CommonActions.ACTION_IMAGES_UPDATEWORKER -> {
                ImageDatabaseWorker.enqueueAction(
                    context,
                    ImageDatabaseWorkerActions.ACTION_UPDATEALARM
                )
            }
            CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION -> {
                UpdaterUtils.enableDailyNotificationService(
                    context,
                    SettingsManager(context.applicationContext).isDailyNotificationEnabled()
                )
            }
            CommonActions.ACTION_WEATHER_LOCATIONREMOVED -> {
                val query = intent.getStringExtra(Constants.WIDGETKEY_LOCATIONQUERY)

                GlobalScope.launch(Dispatchers.Default) {
                    // Get widgets associated with key
                    val ids = WidgetUtils.getWidgetIds(query)

                    ids.forEach { appWidgetId ->
                        if (WidgetUtils.getWidgetTypeFromID(appWidgetId) != WidgetType.Widget4x3Locations) {
                            // Remove location data
                            WidgetUtils.saveLocationData(appWidgetId, null)
                        } else {
                            // Remove old location
                            if (query != null) {
                                val locationSet =
                                    WidgetUtils.getLocationDataSet(appWidgetId)?.minusElement(query)
                                WidgetUtils.saveLocationDataSet(appWidgetId, locationSet)
                            }
                        }
                    }

                    if (query != null) {
                        WidgetUtils.removeLocation(query)
                    }
                }
            }
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.action)
    }
}