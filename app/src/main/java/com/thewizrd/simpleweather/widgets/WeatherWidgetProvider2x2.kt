package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider2x2 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider2x2::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget2x2
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_2x2

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