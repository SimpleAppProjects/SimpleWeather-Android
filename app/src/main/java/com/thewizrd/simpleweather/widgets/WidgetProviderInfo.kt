package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.thewizrd.simpleweather.App

abstract class WidgetProviderInfo {
    // Fields
    abstract val widgetType: WidgetType
    abstract val widgetLayoutId: Int

    val className: String
        get() = this.javaClass.name
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