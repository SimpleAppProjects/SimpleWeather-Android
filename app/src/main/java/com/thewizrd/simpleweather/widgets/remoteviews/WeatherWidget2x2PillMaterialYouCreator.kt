package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider2x2PillMaterialYou
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils

class WeatherWidget2x2PillMaterialYouCreator(context: Context) : WidgetRemoteViewCreator(context) {
    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider2x2PillMaterialYou.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val normalViews =
            buildLayout(R.layout.app_widget_2x2_pill_materialu, location, weather, newOptions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wideViews = buildLayout(
                R.layout.app_widget_2x2_pill_materialu_small,
                location,
                weather,
                newOptions
            )

            val sizes =
                newOptions.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)

            return if (!sizes.isNullOrEmpty()) {
                RemoteViews(sizes.associateWith { size ->
                    if (size.width in 110f..250f && size.height >= 110f) {
                        normalViews
                    } else {
                        wideViews
                    }
                })
            } else {
                normalViews
            }
        } else {
            return normalViews
        }
    }

    private suspend fun buildLayout(
        @LayoutRes layoutId: Int,
        location: LocationData,
        weather: WeatherUiModel,
        newOptions: Bundle
    ): RemoteViews {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val views = RemoteViews(context.packageName, layoutId)

        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        views.setImageViewResource(R.id.weather_icon, weatherIconResId)

        if (!wim.isFontIcon) {
            // Remove tint
            views.setInt(R.id.weather_icon, "setColorFilter", 0x0)
        }

        // Temp
        val temp = weather.curTemp?.removeNonDigitChars()
        val tempStr = if (temp.isNullOrBlank()) {
            WeatherIcons.PLACEHOLDER
        } else {
            "$temp°"
        }

        views.setTextViewText(R.id.condition_temp, tempStr)

        if (layoutId == R.layout.app_widget_2x2_pill_materialu_small) {
            views.setTextViewText(
                R.id.condition_hi,
                if (weather.hiTemp?.isNotBlank() == true) weather.hiTemp else WeatherIcons.PLACEHOLDER
            )
            views.setTextViewText(
                R.id.condition_lo,
                if (weather.loTemp?.isNotBlank() == true) weather.loTemp else WeatherIcons.PLACEHOLDER
            )

            views.setViewVisibility(
                R.id.condition_hilo_layout,
                if (weather.isShowHiLo && cellHeight >= 2) View.VISIBLE else View.GONE
            )
        }

        setOnClickIntent(location, views)

        return views
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // no-op
    }
}