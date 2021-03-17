package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.content.Intent
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterService

class WeatherWidgetProvider4x1Google : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (Intent.ACTION_TIME_CHANGED == action || Intent.ACTION_TIMEZONE_CHANGED == action) {
            WeatherUpdaterService.enqueueWork(context, Intent(context, WeatherUpdaterService::class.java)
                    .setAction(WeatherUpdaterService.ACTION_UPDATEDATE)
                    .putExtra(EXTRA_WIDGET_IDS, info.appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, info.widgetType.value))
        } else {
            super.onReceive(context, intent)
        }
    }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x1Google::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x1Google
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x1_google

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