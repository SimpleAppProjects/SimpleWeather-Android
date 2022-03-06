package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x1Google : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

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