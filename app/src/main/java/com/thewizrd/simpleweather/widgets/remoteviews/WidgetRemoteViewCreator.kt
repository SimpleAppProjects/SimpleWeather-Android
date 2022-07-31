package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
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
                    // Widget dimensions
                    val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

                    val imgWidth = context.dpToPx(minWidth.toFloat()).toInt()
                    val imgHeight = context.dpToPx(minHeight.toFloat()).toInt()

                    updateViews.setImageViewBitmap(
                        R.id.widgetBackground,
                        ImageUtils.fillColorRoundedCornerBitmap(
                            backgroundColor,
                            imgWidth, imgHeight, context.dpToPx(16f)
                        )
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
        if (WidgetUtils.isBackgroundCustomOnlyWidget(WidgetUtils.getWidgetTypeFromID(appWidgetId))) {
            val backgroundColor =
                newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(
                    appWidgetId
                )

            if (backgroundColor != Colors.TRANSPARENT && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Widget dimensions
                val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

                val imgWidth = context.dpToPx(minWidth.toFloat()).toInt()
                val imgHeight = context.dpToPx(minHeight.toFloat()).toInt()

                updateViews.setImageViewBitmap(
                    R.id.widgetBackground,
                    ImageUtils.fillColorRoundedCornerBitmap(
                        backgroundColor,
                        imgWidth, imgHeight, context.dpToPx(16f)
                    )
                )
            }
        }
    }
}