package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.annotation.ColorInt
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.ColorBuilders.ColorProp
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.*
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.ModifiersBuilders.Padding
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX

const val ID_WEATHER_HI_ICON = "hi_icon"
const val ID_WEATHER_LO_ICON = "lo_icon"

internal fun currentWeatherGoogleTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()

    return currentWeatherGoogleTileLayout(
        context,
        deviceParameters,
        location = viewModel?.location ?: WeatherIcons.PLACEHOLDER,
        weatherIconId = "$ID_WEATHER_ICON_PREFIX${viewModel?.weatherIcon ?: WeatherIcons.NA}",
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
        weatherCondition = viewModel?.curCondition ?: WeatherIcons.EM_DASH
    )
}

internal fun currentWeatherGoogleTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    location: String,
    weatherIconId: String,
    currentTemperature: String,
    @ColorInt currentTemperatureColor: Int,
    lowTemperature: String,
    highTemperature: String,
    weatherCondition: String
): LayoutElement = PrimaryLayout.Builder(deviceParameters)
    .setPrimaryLabelTextContent(
        Image.Builder()
            .setWidth(dp(36f))
            .setHeight(dp(36f))
            .setResourceId(weatherIconId)
            .build()
    )
    .setContent(
        Row.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Column.Builder()
                    .addContent(
                        Text.Builder(context, currentTemperature)
                            .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                            .setColor(ColorBuilders.argb(currentTemperatureColor))
                            .build()
                    )
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .build()
            )
            .addContent(
                Spacer.Builder()
                    .setWidth(dp(4f))
                    .build()
            )
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .setHeight(wrap())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                    .addContent(
                        Text.Builder(context, weatherCondition)
                            .setColor(ColorBuilders.argb(Colors.WHITE))
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setMultilineAlignment(TEXT_ALIGN_START)
                            .setOverflow(TEXT_OVERFLOW_ELLIPSIZE_END)
                            .setMaxLines(1)
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, location)
                            .setColor(ColorBuilders.argb(Colors.GRAY))
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setMultilineAlignment(TEXT_ALIGN_START)
                            .setOverflow(TEXT_OVERFLOW_ELLIPSIZE_END)
                            .setMaxLines(1)
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .setSecondaryLabelTextContent(
        Row.Builder()
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Text.Builder(context, highTemperature)
                    .setColor(ColorBuilders.argb(Colors.WHITE))
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .setModifiers(
                        Modifiers.Builder()
                            .setPadding(
                                Padding.Builder()
                                    .setEnd(dp(4f))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                Image.Builder()
                    .setResourceId(ID_WEATHER_HI_ICON)
                    .setHeight(dp(20f))
                    .setWidth(dp(20f))
                    .setColorFilter(
                        ColorFilter.Builder()
                            .setTint(
                                ColorProp.Builder()
                                    .setArgb(Colors.ORANGERED)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                Spacer.Builder()
                    .setWidth(MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH)
                    .build()
            )
            .addContent(
                Text.Builder(context, lowTemperature)
                    .setColor(ColorBuilders.argb(Colors.GRAY))
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .setModifiers(
                        Modifiers.Builder()
                            .setPadding(
                                Padding.Builder()
                                    .setEnd(dp(4f))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                Image.Builder()
                    .setResourceId(ID_WEATHER_LO_ICON)
                    .setHeight(dp(20f))
                    .setWidth(dp(20f))
                    .setColorFilter(
                        ColorFilter.Builder()
                            .setTint(
                                ColorProp.Builder()
                                    .setArgb(Colors.LIGHTSKYBLUE)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .build()