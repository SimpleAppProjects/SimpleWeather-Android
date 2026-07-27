package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.annotation.ColorInt
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.material3.ButtonGroupDefaults.DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.theme.wearTileColorScheme
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX

internal fun currentWeatherTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()

    return currentWeatherTileLayout(
        context,
        deviceParameters,
        location = viewModel?.location ?: WeatherIcons.EM_DASH,
        weatherIconId = "$ID_WEATHER_ICON_PREFIX${viewModel?.weatherIcon ?: WeatherIcons.NA}",
        currentTemperature = viewModel?.curTemp?.replace(viewModel.tempUnit ?: "", "")
            ?: WeatherIcons.EM_DASH,
        currentTemperatureColor = weather?.condition?.tempF?.let {
            getColorFromTempF(
                it,
                Colors.WHITE
            )
        } ?: Colors.WHITE,
        lowTemperature = viewModel?.loTemp ?: WeatherIcons.PLACEHOLDER,
        highTemperature = viewModel?.hiTemp ?: WeatherIcons.PLACEHOLDER,
        showHiLo = viewModel?.isShowHiLo ?: true,
        weatherCondition = viewModel?.curCondition ?: WeatherIcons.EM_DASH
    )
}

internal fun currentWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    location: String,
    weatherIconId: String,
    currentTemperature: String,
    @ColorInt currentTemperatureColor: Int,
    lowTemperature: String,
    highTemperature: String,
    showHiLo: Boolean = true,
    weatherCondition: String
): LayoutElement =
    materialScope(context, deviceParameters, defaultColorScheme = wearTileColorScheme) {
    val iconSize = if (deviceParameters.isLargeWidth()) {
        dp(48f)
    } else {
        dp(32f)
    }

    primaryLayout(
        margins = PrimaryLayoutMargins.MIN_PRIMARY_LAYOUT_MARGIN,
        titleSlot = {
            text(
                modifier = LayoutModifier.padding(4f),
                text = (location.split(',').firstOrNull() ?: location).layoutString
            )
        },
        mainSlot = {
            buttonGroup {
                buttonGroupItem {
                    Column.Builder()
                        .setWidth(expand())
                        .addContent(
                            icon(
                                weatherIconId,
                                width = iconSize,
                                height = iconSize
                            )
                        )
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .build()
                }
                buttonGroupItem {
                    Column.Builder()
                        .setModifiers(
                            Modifiers.Builder()
                                .setPadding(padding(start = 4f))
                                .build()
                        )
                        .setWidth(expand())
                        .addContent(
                            text(
                                text = currentTemperature.layoutString,
                                color = currentTemperatureColor.argb,
                                typography = if (deviceParameters.isLargeWidth()) {
                                    Typography.DISPLAY_LARGE
                                } else {
                                    Typography.DISPLAY_MEDIUM
                                }
                            )
                        )
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .build()
                }
                if (showHiLo) {
                    buttonGroupItem {
                        Column.Builder()
                            .setWidth(expand())
                            .setHeight(wrap())
                            .addContent(
                                text(
                                    highTemperature.layoutString,
                                    typography = Typography.TITLE_MEDIUM,
                                    color = Colors.WHITE.argb
                                )
                            )
                            .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                            .addContent(
                                text(
                                    lowTemperature.layoutString,
                                    typography = Typography.TITLE_MEDIUM,
                                    color = Colors.WHITE.argb
                                )
                            )
                            .build()
                    }
                }
            }
        },
        bottomSlot = {
            text(
                modifier = LayoutModifier.padding(4f),
                text = weatherCondition.layoutString
            )
        }
    )
}

@WearPreviewDevices
private fun currentWeatherTilePreview(context: Context) = TilePreviewData(
    onTileResourceRequest = {
        Resources.Builder()
            .addIdToImageMapping(
                "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    sharedRes.drawable.wi_day_sunny,
                    Colors.WHITE
                ).toImageResource()
            )
            .addIdToImageMapping(
                "$ID_WEATHER_ICON_PREFIX${WeatherIcons.NA}",
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    sharedRes.drawable.wi_na,
                    Colors.WHITE
                ).toImageResource()
            )
            .build()
    },
    onTileRequest = { request ->
        TilePreviewHelper.singleTimelineEntryTileBuilder(
            currentWeatherTileLayout(
                context,
                request.deviceConfiguration,
                location = "New York, New York",
                weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                currentTemperature = "70°",
                currentTemperatureColor = getColorFromTempF(70f),
                lowTemperature = "60°",
                highTemperature = "75°",
                weatherCondition = "Sunny"
            )
        ).build()
    }
)