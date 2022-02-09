package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider3x1MaterialYou : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider3x1MaterialYou::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget3x1MaterialYou
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_3x1_materialu

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