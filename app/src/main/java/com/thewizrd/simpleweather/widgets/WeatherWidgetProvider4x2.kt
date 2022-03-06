package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x2 : WeatherWidgetProvider() {
    companion object {
        private fun getNextIndex(index: Int): Int {
            return (index + 1) % 2
        }
    }

    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (ACTION_SHOWNEXTFORECAST == action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetId =
                intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

            // Note: workaround; possibly temporary?
            val views = RemoteViews(context.packageName, info.widgetLayoutId)
            val index = getNextIndex(WidgetUtils.getDisplayedChild(appWidgetId))
            views.setInt(R.id.forecast_layout, "setDisplayedChild", index)
            WidgetUtils.setDisplayedChild(appWidgetId, index)
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