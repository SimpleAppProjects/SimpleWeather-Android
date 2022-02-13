package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import com.thewizrd.simpleweather.R

class WeatherWidgetProvider4x2ForecastGraph : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x2ForecastGraph::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x2Graph
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x2_graph

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // do nothing; widget will resize itself
    }
}