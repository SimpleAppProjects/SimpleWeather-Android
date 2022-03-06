package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x2Huawei : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x2Huawei::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x2Huawei
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x2_huawei

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