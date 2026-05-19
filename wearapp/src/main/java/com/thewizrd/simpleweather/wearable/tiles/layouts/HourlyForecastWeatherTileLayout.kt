package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import android.text.format.DateFormat
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeWatch
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal fun hourlyForecastWeatherTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.hrForecast?.map { HourlyForecastTileModel(context, LocaleUtils.getLocale(), it) }

    return hourlyForecastWeatherTileLayout(
        context,
        deviceParameters,
        location = viewModel?.location ?: WeatherIcons.EM_DASH,
        weatherIconId = "$ID_WEATHER_ICON_PREFIX${viewModel?.weatherIcon ?: WeatherIcons.NA}",
        currentTemperature = viewModel?.curTemp?.replace(viewModel.tempUnit ?: "", "")
            ?: WeatherIcons.EM_DASH,
        tempHi = viewModel?.hiTemp,
        tempLo = viewModel?.loTemp,
        forecasts = forecasts
    )
}

private fun hourlyForecastWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    location: String,
    weatherIconId: String,
    currentTemperature: String,
    tempHi: String? = null,
    tempLo: String? = null,
    forecasts: List<HourlyForecastTileModel>?
): LayoutElement = PrimaryLayout.Builder(deviceParameters)
    .setResponsiveContentInsetEnabled(true)
    .setPrimaryLabelTextContent(forecastLocation(context, location))
    .setContent(
        Column.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(
                forecastCurrentLayout(
                    context,
                    weatherIconId,
                    currentTemperature,
                    tempHi,
                    tempLo
                )
            )
            .addContent(
                Row.Builder()
                    .setHeight(expand())
                    .setWidth(expand())
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .setModifiers(
                        Modifiers.Builder()
                            .setPadding(Padding.Builder().setTop(dp(4f)).build())
                            .build()
                    )
                    .apply {
                        forecasts?.take(if (context.isLargeWatch()) 4 else 3)?.forEach { item ->
                            addContent(hourlyForecastItemLayout(context, item))
                        }
                    }
                    .build()
            )
            .build()
    )
    .build()

private fun hourlyForecastItemLayout(
    context: Context,
    forecast: HourlyForecastTileModel
): LayoutElement = Column.Builder()
    .setHeight(expand())
    .setWidth(weight(1f))
    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
    .addContent(
        Text.Builder(context, forecast.date)
            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
            .setColor(ColorProp.Builder(Colors.WHITE).build())
            .setMaxLines(1)
            .setWeight(FONT_WEIGHT_NORMAL)
            .build()
    )
    .addContent(
        Image.Builder()
            .setResourceId("${ID_WEATHER_ICON_PREFIX}${forecast.icon ?: WeatherIcons.NA}")
            .setHeight(dp(24f))
            .setWidth(dp(24f))
            .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
            .build()
    )
    .addContent(
        Text.Builder(context, forecast.temp)
            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
            .setColor(ColorProp.Builder(Colors.WHITE).build())
            .setMaxLines(1)
            .setWeight(FONT_WEIGHT_NORMAL)
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
        val value = if (isFahrenheit) forecast.highF.roundToInt() else forecast.highC.roundToInt()
        String.format(locale, "%d°", value)
    }.getOrElse {
        WeatherIcons.PLACEHOLDER
    }
    val condition: String = forecast.condition
}

@WearPreviewDevices
private fun hourlyForecastWeatherTilePreview(context: Context): TilePreviewData {
    val forecasts = MutableList(4) {
        HourlyForecastTileModel(context, Locale.getDefault(), HourlyForecast().apply {
            date = ZonedDateTime.now().plusHours(it.toLong())
            highF = 70f
            highC = ConversionMethods.FtoC(70f)
            icon = WeatherIcons.CLOUDY
            condition = "Cloudy"
        })
    }

    return TilePreviewData(
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
                .addIdToImageMapping(
                    ID_WEATHER_CHANCE_ICON,
                    drawableResToImageResource(sharedRes.drawable.wi_umbrella_white)
                )
                .addIdToImageMapping(
                    ID_WEATHER_CLOUDINESS_ICON,
                    drawableResToImageResource(sharedRes.drawable.wi_cloudy)
                )
                .addIdToImageMapping(
                    ID_WEATHER_WINDSPEED_ICON,
                    drawableResToImageResource(sharedRes.drawable.wi_strong_wind)
                )
                .apply {
                    forecasts.forEach { item ->
                        addIdToImageMapping(
                            "${ID_WEATHER_ICON_PREFIX}${item.icon}",
                            ImageUtils.tintedBitmapFromDrawable(
                                context,
                                sharedRes.drawable.wi_cloudy,
                                Colors.WHITE
                            ).toImageResource()
                        )
                    }
                }
                .build()
        },
        onTileRequest = { request ->
            TilePreviewHelper.singleTimelineEntryTileBuilder(
                hourlyForecastWeatherTileLayout(
                    context,
                    request.deviceConfiguration,
                    location = "New York",
                    weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                    currentTemperature = "70°",
                    tempHi = "75°",
                    tempLo = "60°",
                    forecasts = forecasts
                )
            ).build()
        }
    )
}