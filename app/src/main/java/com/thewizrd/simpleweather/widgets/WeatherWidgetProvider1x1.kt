package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider1x1 : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget1x1Info() }
}

class Widget1x1Info : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget1x1
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_1x1

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget1x1Info()
            }
            return _instance!!
        }
    }
}