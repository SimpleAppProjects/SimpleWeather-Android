package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import android.text.format.DateFormat
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DeviceParametersBuilders.SCREEN_SHAPE_ROUND
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.LayoutDefaults.MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.wearable.tiles.ID_FORECAST_ICON_PREFIX
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun hourlyForecastWeatherTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.hrForecast?.map { HourlyForecastTileModel(context, LocaleUtils.getLocale(), it) }

    // Details
    val popChanceModel = viewModel?.weatherDetailsMap?.get(WeatherDetailsType.POPCHANCE)
    val popCloudinessModel = viewModel?.weatherDetailsMap?.get(WeatherDetailsType.POPCLOUDINESS)
    val windModel = viewModel?.weatherDetailsMap?.get(WeatherDetailsType.WINDSPEED)

    return hourlyForecastWeatherTileLayout(
        context,
        deviceParameters,
        weatherIconId = "$ID_WEATHER_ICON_PREFIX${viewModel?.weatherIcon ?: WeatherIcons.NA}",
        currentTemperature = viewModel?.curTemp?.replace(viewModel.tempUnit ?: "", "")
            ?: WeatherIcons.PLACEHOLDER,
        popChance = popChanceModel?.value?.toString(),
        popCloudiness = popCloudinessModel?.value?.toString(),
        windSpeed = windModel?.value?.toString(),
        forecasts = forecasts
    )
}

internal fun hourlyForecastWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    weatherIconId: String,
    currentTemperature: String,
    popChance: String?,
    popCloudiness: String?,
    windSpeed: String?,
    forecasts: List<HourlyForecastTileModel>?
): LayoutElement = Column.Builder()
    .setWidth(expand())
    .setHeight(expand())
    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
    .setModifiers(
        Modifiers.Builder()
            .setPadding(
                Padding.Builder()
                    .setRtlAware(true)
                    .setTop(
                        dp(
                            deviceParameters.screenHeightDp * if (deviceParameters.screenShape == SCREEN_SHAPE_ROUND) {
                                /*PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT =*/ 16.7f / 100
                            } else {
                                /*PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT =*/ 13.3f / 100
                            }
                        )
                    )
                    .setBottom(
                        dp(
                            deviceParameters.screenHeightDp * if (deviceParameters.screenShape == SCREEN_SHAPE_ROUND) {
                                /*PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT =*/ 2.1f / 100
                            } else {
                                /*PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT =*/ 0f
                            }
                        )
                    )
                    .build()
            )
            .build()
    )
    .addContent(
        Column.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(
                Row.Builder()
                    .setWidth(wrap())
                    .setHeight(wrap())
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .addContent(
                        Image.Builder()
                            .setWidth(dp(36f))
                            .setHeight(dp(36f))
                            .setResourceId(weatherIconId)
                            .build()
                    )
                    .addContent(
                        Spacer.Builder()
                            .setWidth(dp(16f))
                            .setHeight(dp(0f))
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, currentTemperature)
                            .setColor(
                                ColorProp.Builder(Colors.WHITE)
                                    .build()
                            )
                            .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                            .setWeight(FONT_WEIGHT_NORMAL)
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .addContent(
        Row.Builder()
            .setWidth(wrap())
            .setHeight(wrap())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .apply {
                var isEmpty = true

                if (!popChance.isNullOrBlank() || !popCloudiness.isNullOrBlank() || !windSpeed.isNullOrBlank()) {
                    // Add pop content
                    if (!popChance.isNullOrBlank()) {
                        isEmpty = false

                        addContent(
                            Row.Builder()
                                .setWidth(wrap())
                                .setHeight(wrap())
                                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                .addContent(
                                    Image.Builder()
                                        .setWidth(dp(20f))
                                        .setHeight(dp(20f))
                                        .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
                                        .setResourceId(ID_WEATHER_CHANCE_ICON)
                                        .setColorFilter(
                                            ColorFilter.Builder()
                                                .setTint(
                                                    ColorProp.Builder(0xFF589DF2.toInt())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .setModifiers(
                                            Modifiers.Builder()
                                                .setPadding(
                                                    Padding.Builder()
                                                        .setAll(dp(2f))
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    Text.Builder(context, popChance)
                                        .setTypography(Typography.TYPOGRAPHY_CAPTION3)
                                        .setColor(
                                            ColorProp.Builder(0xFF589DF2.toInt())
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    } else if (!popCloudiness.isNullOrBlank()) {
                        isEmpty = false

                        addContent(
                            Row.Builder()
                                .setWidth(wrap())
                                .setHeight(wrap())
                                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                .addContent(
                                    Image.Builder()
                                        .setWidth(dp(20f))
                                        .setHeight(dp(20f))
                                        .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
                                        .setResourceId(ID_WEATHER_CLOUDINESS_ICON)
                                        .setColorFilter(
                                            ColorFilter.Builder()
                                                .setTint(
                                                    ColorProp.Builder(0xFF589DF2.toInt())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .setModifiers(
                                            Modifiers.Builder()
                                                .setPadding(
                                                    Padding.Builder()
                                                        .setAll(dp(2f))
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    Text.Builder(context, popCloudiness)
                                        .setTypography(Typography.TYPOGRAPHY_CAPTION3)
                                        .setColor(
                                            ColorProp.Builder(0xFF589DF2.toInt())
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    }

                    if (!isEmpty) {
                        addContent(
                            Spacer.Builder()
                                .setWidth(MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH)
                                .setHeight(dp(0f))
                                .build()
                        )
                    }

                    // Add Wind icon
                    if (!windSpeed.isNullOrBlank()) {
                        addContent(
                            Row.Builder()
                                .setWidth(wrap())
                                .setHeight(wrap())
                                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                .addContent(
                                    Image.Builder()
                                        .setWidth(dp(20f))
                                        .setHeight(dp(20f))
                                        .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
                                        .setResourceId(ID_WEATHER_WINDSPEED_ICON)
                                        .setColorFilter(
                                            ColorFilter.Builder()
                                                .setTint(
                                                    ColorProp.Builder(0xFF20B2AA.toInt())
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .setModifiers(
                                            Modifiers.Builder()
                                                .setPadding(
                                                    Padding.Builder()
                                                        .setAll(dp(2f))
                                                        .build()
                                                )
                                                .build()
                                        )
                                        .build()
                                )
                                .addContent(
                                    Text.Builder(context, windSpeed)
                                        .setTypography(Typography.TYPOGRAPHY_CAPTION3)
                                        .setColor(
                                            ColorProp.Builder(0xFF20B2AA.toInt())
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    }
                }
            }
            .build()
    )
    .addContent(
        Spacer.Builder()
            .setWidth(dp(0f))
            .setHeight(dp(4f))
            .build()
    )
    .addContent(
        Column.Builder()
            .setHeight(expand())
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(
                Row.Builder()
                    .setHeight(expand())
                    .setWidth(wrap())
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .apply {
                        forecasts?.forEachIndexed { index, item ->
                            addContent(
                                hourlyForecastItemLayout(context, item, index)
                            )
                        }
                    }
                    .build()
            )
            .build()
    )
    .build()

internal fun hourlyForecastItemLayout(
    context: Context,
    forecast: HourlyForecastTileModel,
    index: Int
): LayoutElement = Column.Builder()
    .setHeight(expand())
    .setWidth(wrap())
    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
    .addContent(
        Text.Builder(context, forecast.date)
            .setTypography(Typography.TYPOGRAPHY_CAPTION3)
            .setColor(
                ColorProp.Builder(Colors.WHITE)
                    .build()
            )
            .setMaxLines(1)
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setAll(dp(2f))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .addContent(
        Image.Builder()
            .setResourceId("${ID_FORECAST_ICON_PREFIX}idx=${index}:${forecast.icon ?: WeatherIcons.NA}")
            .setHeight(dp(28f))
            .setWidth(dp(28f))
            .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
            .setModifiers(
                Modifiers.Builder()
                    .setPadding(
                        Padding.Builder()
                            .setAll(dp(2f))
                            .build()
                    )
                    .build()
            )
            .build()
    )
    .addContent(
        Text.Builder(context, forecast.temp)
            .setTypography(Typography.TYPOGRAPHY_CAPTION3)
            .setColor(
                ColorProp.Builder(Colors.WHITE)
                    .build()
            )
            .setMaxLines(1)
            .build()
    )
    .build()

internal class HourlyForecastTileModel(context: Context, locale: Locale, forecast: HourlyForecast) {
    private val isFahrenheit = Units.FAHRENHEIT == SettingsManager(context).getTemperatureUnit()
    val date: String =
        if (DateFormat.is24HourFormat(context)) {
            forecast.date.format(
                DateTimeFormatter.ofPattern(
                    DateTimeUtils.getBestPatternForSkeleton(
                        DateTimeConstants.SKELETON_24HR,
                        locale
                    ), locale
                )
            )
        } else {
            forecast.date.format(
                DateTimeFormatter.ofPattern(
                    DateTimeConstants.ABBREV_12HR_AMPM_SHORT,
                    locale
                )
            )
        }
    val icon: String = forecast.icon
    val temp: String = runCatching {
        val value = if (isFahrenheit) Math.round(forecast.highF) else Math.round(forecast.highC)
        String.format(locale, "%d°", value)
    }.getOrElse {
        WeatherIcons.PLACEHOLDER
    }
}