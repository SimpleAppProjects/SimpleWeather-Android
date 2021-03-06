package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.content.Intent
import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x2Clock : WeatherWidgetProvider() {
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
        } else {
            super.onReceive(context, intent)
        }
    }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x2Clock::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x2Clock
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x2_clock

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