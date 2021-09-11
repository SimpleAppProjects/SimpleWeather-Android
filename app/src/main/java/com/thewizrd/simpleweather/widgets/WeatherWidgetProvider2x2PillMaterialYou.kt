package com.thewizrd.simpleweather.widgets

import com.thewizrd.simpleweather.R

class WeatherWidgetProvider2x2PillMaterialYou : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider2x2PillMaterialYou::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget2x2PillMaterialYou
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_2x2_pill_materialu

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