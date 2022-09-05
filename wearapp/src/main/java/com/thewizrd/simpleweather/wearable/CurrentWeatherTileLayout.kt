package com.thewizrd.simpleweather.wearable

import android.content.Context
import androidx.annotation.ColorInt
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.*
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Weather

internal fun currentWeatherTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()

    return currentWeatherTileLayout(
        context,
        deviceParameters,
        location = viewModel?.location ?: WeatherIcons.PLACEHOLDER,
        weatherIconId = "${ID_WEATHER_ICON_PREFIX}${viewModel?.weatherIcon ?: WeatherIcons.NA}",
        currentTemperature = viewModel?.curTemp?.replace(viewModel.tempUnit ?: "", "")
            ?: WeatherIcons.PLACEHOLDER,
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

private fun currentWeatherTileLayout(
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
): LayoutElement = PrimaryLayout.Builder(deviceParameters)
    .setPrimaryLabelTextContent(
        Text.Builder(context, location)
            .setColor(ColorBuilders.argb(Colors.WHITE))
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .build()
    )
    .setContent(
        Row.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .addContent(
                        Image.Builder()
                            .setWidth(dp(32f))
                            .setHeight(dp(32f))
                            .setResourceId(weatherIconId)
                            .build()
                    )
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .build()
            )
            .addContent(
                Spacer.Builder()
                    .setWidth(MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH)
                    .build()
            )
            .addContent(
                Box.Builder()
                    .setWidth(expand())
                    .setHeight(wrap())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        Text.Builder(context, currentTemperature)
                            .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                            .setColor(ColorBuilders.argb(currentTemperatureColor))
                            .build()
                    )
                    .build()
            )
            .apply {
                if (showHiLo) {
                    addContent(
                        Spacer.Builder()
                            .setWidth(MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH)
                            .build()
                    )

                    addContent(
                        Column.Builder()
                            .setWidth(expand())
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                            .addContent(
                                Text.Builder(context, highTemperature)
                                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                    .setColor(ColorBuilders.argb(Colors.WHITE))
                                    .build()
                            )
                            .addContent(
                                Text.Builder(context, lowTemperature)
                                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                    .setColor(ColorBuilders.argb(Colors.GRAY))
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .build()
    )
    .setSecondaryLabelTextContent(
        Text.Builder(context, weatherCondition)
            .setColor(ColorBuilders.argb(Colors.WHITE))
            .setTypography(Typography.TYPOGRAPHY_TITLE3)
            .build()
    )
    .build()