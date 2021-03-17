package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.thewizrd.simpleweather.App

abstract class WidgetProviderInfo protected constructor() {
    // Fields
    abstract val widgetType: WidgetType
    abstract val widgetLayoutId: Int
    abstract val className: String

    val componentName: ComponentName
        get() = ComponentName(App.instance.appContext, className)

    val appWidgetIds: IntArray
        get() {
            val appWidgetManager = AppWidgetManager.getInstance(App.instance.appContext)
            return appWidgetManager.getAppWidgetIds(componentName)
        }

    val instancesCount: Int
        get() {
            val appWidgetIds = appWidgetIds
            return appWidgetIds.size
        }

    val hasInstances: Boolean
        get() {
            return instancesCount > 0
        }

}