package com.thewizrd.simpleweather.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.simpleweather.services.ImageDatabaseWorker
import com.thewizrd.simpleweather.services.UpdaterUtils.Companion.updateAlarm
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.widgets.WeatherWidgetService
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CommonActionsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CommonActionsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (CommonActions.ACTION_SETTINGS_UPDATEAPI == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE)
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
        } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDUPDATE)
        } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE)
            WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
        } else if (CommonActions.ACTION_SETTINGS_UPDATEREFRESH == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE)
            updateAlarm(context)
        } else if (CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDLOCATIONUPDATE)
            if (intent.getBooleanExtra(CommonActions.EXTRA_FORCEUPDATE, true)) {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
        } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION == intent.action) {
            val oldKey = intent.getStringExtra(Constants.WIDGETKEY_OLDKEY)
            val locationJson = intent.getStringExtra(Constants.WIDGETKEY_LOCATION)

            GlobalScope.launch(Dispatchers.Default) {
                val location = JSONParser.deserializer(locationJson, LocationData::class.java)

                if (WidgetUtils.exists(oldKey)) {
                    WidgetUtils.updateWidgetIds(oldKey, location)
                }
            }
        } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER == intent.action) {
            val locationQuery = intent.getStringExtra(Constants.WIDGETKEY_LOCATIONQUERY)
            val weatherJson = intent.getStringExtra(Constants.WIDGETKEY_WEATHER)

            GlobalScope.launch(Dispatchers.Default) {
                if (WidgetUtils.exists(locationQuery)) {
                    val weather = JSONParser.deserializer(weatherJson, Weather::class.java)

                    val ids = WidgetUtils.getWidgetIds(locationQuery)
                    for (id in ids) {
                        WidgetUtils.saveWeatherData(id, weather)
                    }
                }
            }
        } else if (CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDWEATHERUPDATE)
        } else if (CommonActions.ACTION_WIDGET_RESETWIDGETS == intent.action) {
            WeatherWidgetService.enqueueWork(context, Intent(context, WeatherWidgetService::class.java)
                    .setAction(WeatherWidgetService.ACTION_RESETGPSWIDGETS))
        } else if (CommonActions.ACTION_WIDGET_REFRESHWIDGETS == intent.action) {
            WeatherWidgetService.enqueueWork(context, Intent(context, WeatherWidgetService::class.java)
                    .setAction(WeatherWidgetService.ACTION_REFRESHGPSWIDGETS))
        } else if (CommonActions.ACTION_IMAGES_UPDATEWORKER == intent.action) {
            ImageDatabaseWorker.enqueueAction(context, ImageDatabaseWorker.ACTION_UPDATEALARM)
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.action)
    }
}