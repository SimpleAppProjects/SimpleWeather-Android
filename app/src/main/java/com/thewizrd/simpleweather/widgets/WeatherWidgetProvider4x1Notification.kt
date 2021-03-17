package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x1Notification : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Widget4x1NotificationInfo() }
}

class Widget4x1NotificationInfo : WidgetProviderInfo() {
    // Overrides
    override val widgetType: WidgetType
        get() = WidgetType.Widget4x1Notification
    override val widgetLayoutId: Int
        get() = R.layout.app_widget_4x1_notification

    companion object {
        private var _instance: WidgetProviderInfo? = null

        @JvmStatic
        fun getInstance(): WidgetProviderInfo {
            if (_instance == null) {
                _instance = Widget4x1NotificationInfo()
            }
            return _instance!!
        }
    }
}