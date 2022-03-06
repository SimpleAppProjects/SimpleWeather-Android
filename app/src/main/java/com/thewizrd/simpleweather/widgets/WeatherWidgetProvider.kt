package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.UpdaterUtils
import com.thewizrd.simpleweather.services.WidgetWorker

abstract class WeatherWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "WeatherWidgetProvider"

        // Actions
        const val ACTION_SHOWNEXTFORECAST = "SimpleWeather.Droid.action.SHOW_NEXT_FORECAST"
        const val ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET"

        // Extras
        const val EXTRA_WIDGET_ID = "SimpleWeather.Droid.extra.WIDGET_ID"
        const val EXTRA_WIDGET_IDS = "SimpleWeather.Droid.extra.WIDGET_IDS"
        const val EXTRA_WIDGET_TYPE = "SimpleWeather.Droid.extra.WIDGET_TYPE"
        const val EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME"
        const val EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY"
    }

    protected abstract val info: WidgetProviderInfo

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            Logger.writeLine(
                Log.INFO,
                "%s: WidgetType: %s; onReceive: %s",
                TAG,
                info.widgetType.name,
                intent.action
            )
        }

        if (ACTION_REFRESHWIDGET == intent?.action) {
            val appWidgetId =
                intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            Toast.makeText(
                context,
                context.getString(R.string.action_refresh) + "...",
                Toast.LENGTH_SHORT
            ).show()
            refreshWidget(context, intArrayOf(appWidgetId))
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshWidget(context, appWidgetIds)
    }

    private fun refreshWidget(context: Context, appWidgetIds: IntArray) {
        WidgetWorker.enqueueRefreshWidget(context, appWidgetIds, info, true)
    }

    override fun onEnabled(context: Context) {
        // Show placeholder view
        showLoadingView(context)

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

    private fun showLoadingView(context: Context) {
        AppWidgetManager.getInstance(context).run {
            updateAppWidget(
                info.componentName,
                RemoteViews(context.packageName, R.layout.app_widget_loading_layout)
            )
        }
    }
}