package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.services.UpdaterUtils

abstract class WeatherWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "WeatherWidgetProvider"

        // Actions
        const val ACTION_SHOWNEXTFORECAST = "SimpleWeather.Droid.action.SHOW_NEXT_FORECAST"

        // Extras
        const val EXTRA_WIDGET_ID = "SimpleWeather.Droid.extra.WIDGET_ID"
        const val EXTRA_WIDGET_IDS = "SimpleWeather.Droid.extra.WIDGET_IDS"
        const val EXTRA_WIDGET_OPTIONS = "SimpleWeather.Droid.extra.WIDGET_OPTIONS"
        const val EXTRA_WIDGET_TYPE = "SimpleWeather.Droid.extra.WIDGET_TYPE"
        const val EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME"
        const val EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY"
    }

    protected abstract val info: WidgetProviderInfo

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            Logger.writeLine(Log.INFO, "%s: WidgetType: %s; onReceive: %s", TAG, info.widgetType.name, intent.action)
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WeatherWidgetService.enqueueWork(context, Intent(context, WeatherWidgetService::class.java)
                .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))
    }

    override fun onEnabled(context: Context) {
        // Schedule alarms/updates
        UpdaterUtils.startAlarm(context)
    }

    override fun onDisabled(context: Context) {
        // Remove alarms/updates
        UpdaterUtils.cancelAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetUpdaterHelper.resizeWidget(context, info, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            // Remove id from list
            WidgetUtils.deleteWidget(id)
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        for (i in oldWidgetIds.indices) {
            // Remap widget ids
            WidgetUtils.remapWidget(oldWidgetIds[i], newWidgetIds[i])
        }
    }
}