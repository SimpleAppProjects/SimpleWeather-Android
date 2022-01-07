package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x2 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (Intent.ACTION_TIME_CHANGED == action || Intent.ACTION_TIMEZONE_CHANGED == action) {
            WeatherWidgetService.enqueueWork(context, Intent(context, WeatherWidgetService::class.java)
                    .setAction(WeatherWidgetService.ACTION_UPDATECLOCK)
                    .putExtra(EXTRA_WIDGET_IDS, info.appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))

            WeatherWidgetService.enqueueWork(context, Intent(context, WeatherWidgetService::class.java)
                    .setAction(WeatherWidgetService.ACTION_UPDATEDATE)
                    .putExtra(EXTRA_WIDGET_IDS, info.appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))
        } else if (ACTION_SHOWNEXTFORECAST == action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, info.widgetLayoutId)
            views.showNext(R.id.forecast_layout)
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        } else {
            super.onReceive(context, intent)
        }
    }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x2::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x2
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x2

        companion object {
            private var _instance: WidgetProviderInfo? = null

            @JvmStatic
            fun getInstance(): WidgetProviderInfo {
                if (_instance == null) {
                    _instance = Info()
                }
                return _instance!!
            }
        }
    }
}