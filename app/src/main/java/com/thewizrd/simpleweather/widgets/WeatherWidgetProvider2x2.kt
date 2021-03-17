package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.content.Intent
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterService

class WeatherWidgetProvider2x2 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget2x2Info() }

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
        } else {
            super.onReceive(context, intent)
        }
    }
}

class Widget2x2Info : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget2x2
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_2x2

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget2x2Info()
            }
            return _instance!!
        }
    }
}