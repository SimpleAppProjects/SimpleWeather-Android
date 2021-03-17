package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterService

class WeatherWidgetProvider4x2 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget4x2Info() }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (Intent.ACTION_TIME_CHANGED == action || Intent.ACTION_TIMEZONE_CHANGED == action) {
            WeatherUpdaterService.enqueueWork(context, Intent(context, WeatherUpdaterService::class.java)
                    .setAction(WeatherUpdaterService.ACTION_UPDATECLOCK)
                    .putExtra(EXTRA_WIDGET_IDS, info.appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))

            WeatherUpdaterService.enqueueWork(context, Intent(context, WeatherUpdaterService::class.java)
                    .setAction(WeatherUpdaterService.ACTION_UPDATEDATE)
                    .putExtra(EXTRA_WIDGET_IDS, info.appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))
        } else if (ACTION_SHOWNEXTFORECAST == action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, info.widgetLayoutId)
            views.showNext(R.id.forecast_layout)
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } else {
            super.onReceive(context, intent)
        }
    }
}

class Widget4x2Info : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget4x2
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_4x2

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget4x2Info()
            }
            return _instance!!
        }
    }
}