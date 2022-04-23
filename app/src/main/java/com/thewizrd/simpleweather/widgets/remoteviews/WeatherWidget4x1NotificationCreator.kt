package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x1Notification
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import java.util.*

class WeatherWidget4x1NotificationCreator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews() =
        RemoteViews(context.packageName, R.layout.app_widget_4x1_notification)

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x1Notification.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor = WidgetUtils.getTextColor(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)
        val icoSizeMultiplier = WidgetUtils.getCustomIconSizeMultiplier(appWidgetId)

        // WeatherIcon
        val wim = sharedDeps.weatherIconsManager
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        val weatherIconSize =
            context.resources.getDimension(R.dimen.not_weather_icon_size) * icoSizeMultiplier

        updateViews.setInt(R.id.weather_icon, "setMaxWidth", weatherIconSize.toInt())
        updateViews.setInt(R.id.weather_icon, "setMaxHeight", weatherIconSize.toInt())

        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                viewCtx,
                weatherIconResId,
                weatherIconSize,
                weatherIconSize
            )
        )

        val tempArrowIconSize = context.dpToPx(16f) * txtSizeMultiplier

        // Extra layout
        updateViews.setImageViewBitmap(
            R.id.hi_icon,
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_direction_up,
                textColor,
                tempArrowIconSize,
                tempArrowIconSize
            )
        )
        updateViews.setImageViewBitmap(
            R.id.lo_icon,
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_direction_down,
                textColor,
                tempArrowIconSize,
                tempArrowIconSize
            )
        )

        updateViews.setTextColor(R.id.condition_hi, textColor)
        updateViews.setTextColor(R.id.divider, textColor)
        updateViews.setTextColor(R.id.condition_lo, textColor)
        updateViews.setTextColor(R.id.condition_weather, textColor)
        updateViews.setTextColor(R.id.weather_pop, textColor)
        updateViews.setTextColor(R.id.weather_windspeed, textColor)
        updateViews.setTextColor(R.id.location_name, textColor)

        val tint = if (wim.isFontIcon) textColor else Colors.TRANSPARENT
        updateViews.setInt(R.id.hi_icon, "setColorFilter", tint)
        updateViews.setInt(R.id.lo_icon, "setColorFilter", tint)
        updateViews.setInt(R.id.weather_popicon, "setColorFilter", tint)
        updateViews.setInt(R.id.weather_windicon, "setColorFilter", tint)

        val chanceModel = weather.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
            ?: weather.weatherDetailsMap[WeatherDetailsType.POPCLOUDINESS]
        val windModel = weather.weatherDetailsMap[WeatherDetailsType.WINDSPEED]
        val extraIconSize = context.dpToPx(24f) * txtSizeMultiplier

        if (chanceModel != null) {
            updateViews.setInt(R.id.weather_popicon, "setMaxWidth", extraIconSize.toInt())
            updateViews.setInt(R.id.weather_popicon, "setMaxHeight", extraIconSize.toInt())

            updateViews.setImageViewBitmap(
                R.id.weather_popicon,
                ImageUtils.bitmapFromDrawable(
                    viewCtx,
                    wim.getWeatherIconResource(chanceModel.icon),
                    extraIconSize,
                    extraIconSize
                )
            )
        }

        if (windModel != null) {
            updateViews.setInt(R.id.weather_windicon, "setMaxWidth", extraIconSize.toInt())
            updateViews.setInt(R.id.weather_windicon, "setMaxHeight", extraIconSize.toInt())

            updateViews.setImageViewBitmap(
                R.id.weather_windicon,
                ImageUtils.rotateBitmap(
                    ImageUtils.bitmapFromDrawable(
                        viewCtx,
                        wim.getWeatherIconResource(windModel.icon),
                        extraIconSize,
                        extraIconSize
                    ),
                    windModel.iconRotation.toFloat()
                )
            )
        }

        // Condition text
        updateViews.setTextViewText(
            R.id.condition_weather, String.format(
                Locale.ROOT, "%s - %s",
                if (weather.curTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.curTemp,
                weather.curCondition
            )
        )
        updateViews.setTextViewText(
            R.id.condition_hi,
            if (weather.hiTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.hiTemp
        )
        updateViews.setTextViewText(
            R.id.condition_lo,
            if (weather.loTemp.isNullOrBlank()) WeatherIcons.PLACEHOLDER else weather.loTemp
        )
        updateViews.setViewVisibility(
            R.id.condition_hilo_layout,
            if (weather.isShowHiLo) View.VISIBLE else View.GONE
        )

        if (chanceModel != null) {
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
        }

        if (windModel != null) {
            var speed = if (windModel.value.isNullOrEmpty()) "" else windModel.value.toString()
            speed = speed.split(",").toTypedArray()[0]
            updateViews.setTextViewText(R.id.weather_windspeed, speed)
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE)
        }

        updateViews.setViewVisibility(
            R.id.extra_layout,
            if (chanceModel != null || windModel != null) View.VISIBLE else View.GONE
        )

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        // original icon size: 24dp
        val scaledIconSize = (context.dpToPx(16f) * txtSizeMultiplier).toInt()

        // Refresh icon
        updateViews.setImageViewBitmap(R.id.refresh_button, null)

        updateViews.setInt(R.id.refresh_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.refresh_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.refresh_button, R.drawable.ic_refresh)

        // Setting icon
        updateViews.setImageViewBitmap(R.id.settings_button, null)

        updateViews.setInt(R.id.settings_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.settings_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.settings_button, R.drawable.ic_outline_settings_24)

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.location)

        updateViews.setViewVisibility(
            R.id.location_name,
            if (WidgetUtils.isLocationNameHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (WidgetUtils.isSettingsButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (WidgetUtils.isRefreshButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        updateViewSizes(updateViews, appWidgetId, newOptions)

        return updateViews
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val updateViews = generateRemoteViews()

        updateViewSizes(updateViews, appWidgetId, newOptions)

        resizeWidgetBackground(info, appWidgetId, updateViews, newOptions)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
    }

    private fun updateViewSizes(
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)

        val largeText = cellHeight > 2

        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 16f else 14f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_weather,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 15f else 13f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_hi,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.divider,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.condition_lo,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.weather_pop,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setTextViewTextSize(
            R.id.weather_windspeed,
            TypedValue.COMPLEX_UNIT_SP,
            (if (largeText) 14f else 12f) * txtSizeMultiplier
        )
        updateViews.setViewVisibility(
            R.id.extra_layout,
            if (cellWidth <= 3) View.GONE else View.VISIBLE
        )
    }
}