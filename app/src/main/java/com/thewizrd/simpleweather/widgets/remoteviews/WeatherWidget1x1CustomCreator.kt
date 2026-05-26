package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.TextUtils.applySpan
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider1x1Custom
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.preferences.KEY_BGCOLORCODE
import com.thewizrd.simpleweather.widgets.preferences.KEY_HIDELOCNAME
import com.thewizrd.simpleweather.widgets.preferences.KEY_HIDEREFRESHBTN
import com.thewizrd.simpleweather.widgets.preferences.KEY_HIDESETTINGSBTN
import com.thewizrd.simpleweather.widgets.preferences.KEY_ICONSIZE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TEXTSIZE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TXTCOLORCODE
import com.thewizrd.simpleweather.widgets.preferences.KEY_TXTSHADOW
import com.thewizrd.simpleweather.widgets.preferences.KEY_WEATHERDETAILSTYPEOPTION
import com.thewizrd.shared_resources.R as sharedRes

class WeatherWidget1x1CustomCreator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews(): RemoteViews {
        return RemoteViews(context.packageName, info.widgetLayoutId)
    }

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider1x1Custom.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherUiModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = generateRemoteViews()

        val backgroundColor =
            newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(appWidgetId)
        val textColor =
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        val useTextShadow =
            newOptions.get(KEY_TXTSHADOW) as? Boolean ?: WidgetUtils.useTextShadow(appWidgetId)
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )
        val textAppearanceSpan = if (useTextShadow) {
            TextAppearanceSpan(viewCtx, sharedRes.style.ShadowText)
        } else {
            null
        }

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        // original icon size: 24dp
        val scaledIconSize = (context.dpToPx(16f) * txtSizeMultiplier).toInt()

        // Refresh icon
        updateViews.setImageViewBitmap(R.id.refresh_button, null)

        updateViews.setInt(R.id.refresh_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.refresh_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.refresh_button, sharedRes.drawable.ic_refresh)

        // Setting icon
        updateViews.setImageViewBitmap(R.id.settings_button, null)

        updateViews.setInt(R.id.settings_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.settings_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(
            R.id.settings_button,
            sharedRes.drawable.ic_outline_settings_24
        )

        // Location Name
        updateViews.setTextViewText(
            R.id.location_name,
            weather.location?.applySpan(textAppearanceSpan)
        )
        updateViews.setTextColor(R.id.location_name, textColor)

        // WeatherDetailsType
        val detailsType =
            newOptions.getSerializable(KEY_WEATHERDETAILSTYPEOPTION) as? WeatherDetailsType
                ?: WidgetUtils.getWidgetDetailsType(appWidgetId)

        var detailItemModel = weather.weatherDetailsMap[detailsType]
        val notAvailValue: CharSequence = context.getString(sharedRes.string.weather_notavailable)

        when (detailsType) {
            WeatherDetailsType.SUNRISE -> {
                if (detailItemModel == null) {
                    detailItemModel =
                        DetailItemViewModel(detailsType, weather.sunPhase?.sunrise ?: notAvailValue)
                }
            }

            WeatherDetailsType.SUNSET -> {
                if (detailItemModel == null) {
                    detailItemModel =
                        DetailItemViewModel(detailsType, weather.sunPhase?.sunset ?: notAvailValue)
                }
            }

            WeatherDetailsType.MOONPHASE -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.moonPhase?.moonPhase ?: DetailItemViewModel(
                        detailsType,
                        notAvailValue
                    )
                }
            }

            WeatherDetailsType.BEAUFORT -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.beaufort?.beaufort ?: DetailItemViewModel(
                        detailsType,
                        notAvailValue
                    )
                }
            }

            WeatherDetailsType.UV -> {
                if (detailItemModel == null) {
                    detailItemModel =
                        weather.uvIndex?.index?.toFloat()?.let { DetailItemViewModel(UV(it)) }
                            ?: DetailItemViewModel(detailsType, notAvailValue)
                }
            }

            WeatherDetailsType.AIRQUALITY -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.airQuality?.airQuality ?: DetailItemViewModel(
                        detailsType,
                        notAvailValue
                    )
                }
            }

            WeatherDetailsType.TREEPOLLEN -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.pollen?.let { pollenVM ->
                        DetailItemViewModel(
                            WeatherDetailsType.TREEPOLLEN,
                            pollenVM.treePollenDesc.toString(),
                            pollenVM.treePollenShortDesc.toString(),
                            0
                        )
                    }
                }
            }

            WeatherDetailsType.GRASSPOLLEN -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.pollen?.let { pollenVM ->
                        DetailItemViewModel(
                            WeatherDetailsType.GRASSPOLLEN,
                            pollenVM.grassPollenDesc.toString(),
                            pollenVM.grassPollenShortDesc.toString(),
                            0
                        )
                    }
                }
            }

            WeatherDetailsType.RAGWEEDPOLLEN -> {
                if (detailItemModel == null) {
                    detailItemModel = weather.pollen?.let { pollenVM ->
                        DetailItemViewModel(
                            WeatherDetailsType.RAGWEEDPOLLEN,
                            pollenVM.ragweedPollenDesc.toString(),
                            pollenVM.ragweedPollenShortDesc.toString(),
                            0
                        )
                    }
                }
            }

            else -> {
                if (detailItemModel == null) {
                    detailItemModel = DetailItemViewModel(detailsType, notAvailValue)
                }
            }
        }

        val wim = sharedDeps.weatherIconsManager
        // icon size: 36dp
        val maxIconSize = context.dpToPx(36f) * icoSizeMultiplier
        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.rotateBitmap(
                ImageUtils.bitmapFromDrawable(
                    viewCtx,
                    wim.getWeatherIconResource(detailItemModel?.icon ?: WeatherIcons.NA),
                    maxIconSize,
                    maxIconSize
                ),
                detailItemModel?.iconRotation?.toFloat() ?: 0f
            )
        )
        if (wim.isFontIcon) {
            updateViews.setInt(R.id.weather_icon, "setColorFilter", textColor)
        } else {
            updateViews.setInt(R.id.weather_icon, "setColorFilter", 0)
        }
        updateViews.setContentDescription(
            R.id.weather_icon,
            detailItemModel?.label
        )

        updateViews.setTextViewText(R.id.condition_title, detailItemModel?.label)
        updateViews.setTextViewText(R.id.condition_value, detailItemModel?.shortValue)

        updateViews.setTextColor(R.id.condition_title, textColor)
        updateViews.setTextColor(R.id.condition_value, textColor)

        // Visibility
        updateViews.setViewVisibility(
            R.id.location_name,
            if (newOptions.get(KEY_HIDELOCNAME) as? Boolean ?: WidgetUtils.isLocationNameHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )
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

        // Resizing
        updateViewSizes(updateViews, appWidgetId, newOptions)

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

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

        val txtSizeMultiplier: Float =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        if (cellWidth > 1 && cellHeight > 1) {
            updateViews.setTextViewTextSize(
                R.id.location_name,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )
            updateViews.setTextViewTextSize(
                R.id.condition_title,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )
        } else {
            updateViews.setTextViewTextSize(
                R.id.location_name,
                TypedValue.COMPLEX_UNIT_SP,
                12f * txtSizeMultiplier
            )
            updateViews.setTextViewTextSize(
                R.id.condition_title,
                TypedValue.COMPLEX_UNIT_SP,
                12f * txtSizeMultiplier
            )
        }
        if (cellWidth > 2 && cellHeight > 2) {
            updateViews.setTextViewTextSize(
                R.id.condition_value,
                TypedValue.COMPLEX_UNIT_SP,
                24f * txtSizeMultiplier
            )
        } else if (cellWidth > 1 && cellHeight > 1) {
            updateViews.setTextViewTextSize(
                R.id.condition_value,
                TypedValue.COMPLEX_UNIT_SP,
                18f * txtSizeMultiplier
            )
        } else {
            updateViews.setTextViewTextSize(
                R.id.condition_value,
                TypedValue.COMPLEX_UNIT_SP,
                16f * txtSizeMultiplier
            )
        }
    }
}