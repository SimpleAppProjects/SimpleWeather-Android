package com.thewizrd.simpleweather.widgets.remoteviews

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGCOLORCODE

abstract class WidgetRemoteViewCreator(context: Context) :
    AbstractWidgetRemoteViewCreator(context) {
    override suspend fun buildUpdate(appWidgetId: Int, newOptions: Bundle): RemoteViews? {
        val locData = getLocation(appWidgetId) ?: return null
        val weather = loadWeather(locData) ?: return null
        val viewModel = WeatherUiModel(weather)

        return buildUpdate(appWidgetId, viewModel, locData, newOptions).apply {
            buildExtras(appWidgetId, this, viewModel, locData, newOptions)
        }
    }

    abstract suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews

    open suspend fun buildExtras(
        appWidgetId: Int,
        updateViews: RemoteViews,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ) {
        if (WidgetUtils.isBackgroundCustomOnlyWidget(WidgetUtils.getWidgetTypeFromID(appWidgetId))) {
            val backgroundColor =
                newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(
                    appWidgetId
                )

            if (backgroundColor == Colors.TRANSPARENT) {
                updateViews.setImageViewBitmap(R.id.widgetBackground, null)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    updateViews.setImageViewBitmap(
                        R.id.widgetBackground,
                        ImageUtils.createColorBitmap(backgroundColor)
                    )
                } else {
                    updateViews.setImageViewResource(
                        R.id.widgetBackground,
                        R.drawable.app_widget_background
                    )
                    updateViews.setInt(
                        R.id.widgetBackground,
                        "setImageAlpha",
                        backgroundColor.alpha
                    )
                    updateViews.setInt(
                        R.id.widgetBackground,
                        "setColorFilter",
                        ColorUtils.setAlphaComponent(backgroundColor, 0xFF)
                    )
                }
            }
        }
    }

    protected open fun resizeWidgetBackground(
        info: WidgetProviderInfo,
        appWidgetId: Int,
        updateViews: RemoteViews,
        newOptions: Bundle
    ) {
    }
}