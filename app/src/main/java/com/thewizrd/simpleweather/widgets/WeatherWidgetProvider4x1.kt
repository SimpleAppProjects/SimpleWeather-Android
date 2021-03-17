package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x1 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget4x1Info.getInstance() }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (intent != null) {
            if (ACTION_SHOWNEXTFORECAST == action) {
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
}

class Widget4x1Info : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget4x1
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_4x1

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget4x1Info()
            }
            return _instance!!
        }
    }
}