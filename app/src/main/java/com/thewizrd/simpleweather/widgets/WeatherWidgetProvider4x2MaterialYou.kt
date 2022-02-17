package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WidgetWorker

class WeatherWidgetProvider4x2MaterialYou : WeatherWidgetProvider() {
    override val info: WidgetProviderInfo by lazy { Info.getInstance() }

    class Info private constructor() : WidgetProviderInfo() {
        override val className: String
            get() = WeatherWidgetProvider4x2MaterialYou::class.java.name
        override val widgetType: WidgetType
            get() = WidgetType.Widget4x2MaterialYou
        override val widgetLayoutId: Int
            get() = R.layout.app_widget_4x2_materialu

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Note: Needed for responsive widget layout
            WidgetWorker.enqueueRefreshWidget(context, intArrayOf(appWidgetId), info, true)
        } else {
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        }
    }
}