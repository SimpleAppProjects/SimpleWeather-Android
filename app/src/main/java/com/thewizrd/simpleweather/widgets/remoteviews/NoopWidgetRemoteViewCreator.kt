package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetType

class NoopWidgetRemoteViewCreator(context: Context) : AbstractWidgetRemoteViewCreator(context) {
    override val info: WidgetProviderInfo
        get() = object : WidgetProviderInfo() {
            override val widgetType: WidgetType
                get() = WidgetType.Unknown
            override val widgetLayoutId: Int
                get() = AppWidgetManager.INVALID_APPWIDGET_ID
            override val className: String
                get() = ""
        }

    override suspend fun buildUpdate(
        appWidgetId: Int,
        newOptions: Bundle
    ): RemoteViews? = null

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
    }
}