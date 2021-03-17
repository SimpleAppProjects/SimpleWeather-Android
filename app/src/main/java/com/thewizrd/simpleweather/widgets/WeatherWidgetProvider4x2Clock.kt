package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.content.Intent
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterService

class WeatherWidgetProvider4x2Clock : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget4x2ClockInfo() }

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

class Widget4x2ClockInfo : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget4x2Clock
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_4x2_clock

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget4x2ClockInfo()
            }
            return _instance!!
        }
    }
}