package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.TextUtils.getTextBounds
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x3Locations
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper.getResIdentifier
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeatherWidget4x3LocationsCreator(context: Context) :
    AbstractWidgetRemoteViewCreator(context) {
    companion object {
        private const val MAX_LOCATION_ITEMS = 5
    }

    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.app_widget_4x3_locations)
    }

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x3Locations.Info.getInstance()

    override suspend fun buildUpdate(appWidgetId: Int, newOptions: Bundle): RemoteViews? {
        val locationSet = WidgetUtils.getLocationDataSet(appWidgetId) ?: return null
        val locations = List(locationSet.size) {
            val location = locationSet.elementAt(it)
            if (location == Constants.KEY_GPS) {
                settingsManager.getHomeData()
            } else {
                settingsManager.getLocation(location)
            }
        }

        val weather = List(locations.size) {
            val l = locations[it]
            l?.let { loc ->
                val w = loadWeather(loc)
                w?.let { data ->
                    WeatherNowViewModel(data)
                }
            }
        }

        return buildUpdate(appWidgetId, locations, weather, newOptions)
    }

    suspend fun buildUpdate(
        appWidgetId: Int, locations: List<LocationData?>,
        weatherData: List<WeatherNowViewModel?>, newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, locations, weatherData, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int, locations: List<LocationData?>,
        weatherData: List<WeatherNowViewModel?>, newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val backgroundColor =
            newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor =
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        // Background
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

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(
            R.id.date_panel,
            getCalendarAppIntent()
        )

        // Open default clock/calendar app
        updateViews.setOnClickPendingIntent(
            R.id.clock_panel,
            getClockAppIntent()
        )

        // Locations
        for (i in 0 until MAX_LOCATION_ITEMS) {
            val location = locations.elementAtOrNull(i)
            val weather = weatherData.elementAtOrNull(i)

            val locationItemId = getResIdentifier(
                R.id::class.java,
                "location${i + 1}"
            ) ?: continue
            val locationNameViewId =
                getResIdentifier(R.id::class.java, "location${i + 1}_name") ?: continue
            val forecastIconId =
                getResIdentifier(R.id::class.java, "forecast${i + 1}_icon") ?: continue
            val forecastHiId = getResIdentifier(
                R.id::class.java,
                "forecast${i + 1}_hi"
            ) ?: continue
            val forecastLoId = getResIdentifier(
                R.id::class.java,
                "forecast${i + 1}_lo"
            ) ?: continue

            if (location == null || weather == null) {
                updateViews.setViewVisibility(locationItemId, View.GONE)
                continue
            }

            updateViews.setTextViewText(locationNameViewId, location.name)
            updateViews.setTextViewText(forecastHiId, weather.hiTemp)
            updateViews.setTextViewText(forecastLoId, weather.loTemp)

            updateViews.setTextColor(locationNameViewId, textColor)
            updateViews.setTextColor(forecastHiId, textColor)
            updateViews.setTextColor(forecastLoId, textColor)

            updateViews.setTextViewTextSize(
                forecastHiId,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )
            updateViews.setTextViewTextSize(
                forecastLoId,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )

            updateViews.setOnClickPendingIntent(
                locationItemId,
                getOnClickIntent(location)
            )

            val wim = sharedDeps.weatherIconsManager
            val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

            val weatherIconSize = context.dpToPx(36f) * icoSizeMultiplier

            updateViews.setInt(forecastIconId, "setMaxWidth", weatherIconSize.toInt())
            updateViews.setInt(forecastIconId, "setMaxHeight", weatherIconSize.toInt())

            updateViews.setImageViewBitmap(
                forecastIconId,
                ImageUtils.bitmapFromDrawable(
                    viewCtx,
                    weatherIconResId,
                    weatherIconSize,
                    weatherIconSize
                )
            )
            if (wim.isFontIcon) {
                updateViews.setInt(forecastIconId, "setColorFilter", textColor)
            } else {
                updateViews.setInt(forecastIconId, "setColorFilter", 0)
            }

            updateViews.setViewVisibility(locationItemId, View.VISIBLE)
        }

        WidgetUtils.setMaxForecastLength(appWidgetId, locations.filterNotNull().size)

        // Date + Time
        updateDateSize(updateViews, appWidgetId, newOptions)
        updateClockSize(updateViews, appWidgetId, newOptions)

        updateViewSizes(updateViews, appWidgetId, newOptions)

        // Color/tint
        updateViews.setTextColor(R.id.clock_panel, textColor)
        updateViews.setTextColor(R.id.date_panel, textColor)

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

        updateViews.setViewVisibility(
            R.id.settings_button,
            if (newOptions.get(KEY_HIDESETTINGSBTN) as? Boolean
                    ?: WidgetUtils.isSettingsButtonHidden(appWidgetId)
            ) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (newOptions.get(KEY_HIDEREFRESHBTN) as? Boolean ?: WidgetUtils.isRefreshButtonHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )

        setOnClickIntent(null, updateViews)
        setOnSettingsClickIntent(updateViews, null, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        return updateViews
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        // Set sizes for views
        updateViewSizes(updateViews, appWidgetId, newOptions)

        // Rebuild extras
        updateClockSize(updateViews, appWidgetId, newOptions)
        updateDateSize(updateViews, appWidgetId, newOptions)

        resizeWidgetBackground(appWidgetId, updateViews, newOptions)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
    }

    private fun resizeWidgetBackground(
        appWidgetId: Int,
        updateViews: RemoteViews,
        newOptions: Bundle
    ) {
        val backgroundColor =
            newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(appWidgetId)

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

    private fun updateViewSizes(
        updateViews: RemoteViews,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        val clockSizeBounds =
            "3:00".getTextBounds(context, (if (isSmallHeight && cellHeight <= 2) 60f else 66f))
        val dateSizeBounds = "Sun, Oct 24".getTextBounds(context, 14f * txtSizeMultiplier)
        val locationsContainerHeight =
            context.dpToPx(minHeight.toFloat()) - clockSizeBounds.height() - dateSizeBounds.height()

        if (locationsContainerHeight <= 0) {
            updateViews.setViewVisibility(R.id.locations_container, View.GONE)
        } else {
            updateViews.setViewVisibility(R.id.locations_container, View.VISIBLE)

            val maxAmountToFit =
                max(1f, locationsContainerHeight / context.dpToPx(36f))
            val maxForecastLength =
                min(maxAmountToFit.roundToInt(), WidgetUtils.getMaxForecastLength(appWidgetId))

            val locationFontSize =
                (if (cellHeight >= 3 && cellWidth > 3) 16f else 14f) * txtSizeMultiplier

            for (i in 0 until MAX_LOCATION_ITEMS) {
                val locationViewId = getResIdentifier(R.id::class.java, "location${i + 1}") ?: 0
                val locationNameViewId =
                    getResIdentifier(R.id::class.java, "location${i}_name") ?: continue

                if (locationViewId != 0) {
                    updateViews.setViewVisibility(
                        locationViewId,
                        if (i < maxForecastLength) View.VISIBLE else View.GONE
                    )
                }

                if (locationNameViewId != 0) {
                    updateViews.setTextViewTextSize(
                        locationNameViewId,
                        TypedValue.COMPLEX_UNIT_SP,
                        locationFontSize
                    )
                }
            }
        }
    }

    private fun updateClockSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        // Update clock widgets
        val timeStr12hr = SpannableString(context.getText(R.string.clock_12_hours_format))
        views.setCharSequence(
            R.id.clock_panel, "setFormat12Hour",
            timeStr12hr
        )
        views.setCharSequence(
            R.id.clock_panel, "setFormat24Hour",
            context.getText(R.string.clock_24_hours_format)
        )

        views.setTextViewTextSize(
            R.id.clock_panel,
            TypedValue.COMPLEX_UNIT_SP,
            (if (isSmallHeight && cellHeight <= 2) 60f else 66f) * txtSizeMultiplier
        )
    }

    private fun updateDateSize(views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        views.setTextViewTextSize(
            R.id.date_panel,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )

        val datePattern = if (cellWidth >= 4) {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT)
        } else {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT)
        }
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }
}