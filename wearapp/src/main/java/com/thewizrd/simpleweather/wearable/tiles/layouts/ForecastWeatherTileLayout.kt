package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiSlotLayout
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
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val ID_WEATHER_CHANCE_ICON = "chance_icon"
const val ID_WEATHER_CLOUDINESS_ICON = "cloudiness_icon"
const val ID_WEATHER_WINDSPEED_ICON = "windspeed_icon"

internal fun forecastWeatherTileLayout(
    weather: Weather?,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.forecast?.map { ForecastTileModel(context, LocaleUtils.getLocale(), it) }

    return forecastWeatherTileLayout(
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

internal fun forecastWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    location: String,
    weatherIconId: String,
    currentTemperature: String,
    tempHi: String? = null,
    tempLo: String? = null,
    forecasts: List<ForecastTileModel>?
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
                            addContent(forecastItemLayout(context, item))
                        }
                    }
                    .build()
            )
            .build()
    )
    .build()

internal fun forecastLocation(
    context: Context,
    location: String
): LayoutElement {
    return Text.Builder(context, location.split(',').firstOrNull() ?: location)
        .setTypography(Typography.TYPOGRAPHY_BODY1)
        .setColor(ColorProp.Builder(Colors.WHITE).build())
        .setMaxLines(1)
        .build()
}

internal fun forecastCurrentLayout(
    context: Context,
    weatherIconId: String,
    currentTemperature: String,
    tempHi: String? = null,
    tempLo: String? = null,
): LayoutElement {
    return MultiSlotLayout.Builder()
        .setHorizontalSpacerWidth(16f)
        .addSlotContent(
            Box.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    Image.Builder()
                        .setWidth(dp(36f))
                        .setHeight(dp(36f))
                        .setResourceId(weatherIconId)
                        .build()
                )
                .build()
        )
        .addSlotContent(
            Box.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    Text.Builder(context, currentTemperature)
                        .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                        .setMultilineAlignment(TEXT_ALIGN_CENTER)
                        .setWeight(FONT_WEIGHT_NORMAL)
                        .setScalable(true)
                        .setMaxLines(1)
                        .setColor(ColorProp.Builder(Colors.WHITE).build())
                        .build()
                )
                .build()
        )
        .apply {
            if (tempHi != null || tempLo != null) {
                addSlotContent(
                    Column.Builder()
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            Text.Builder(context, tempHi ?: WeatherIcons.PLACEHOLDER)
                                .setTypography(Typography.TYPOGRAPHY_BODY2)
                                .setColor(ColorProp.Builder(Colors.WHITE).build())
                                .setMaxLines(1)
                                .build()
                        )
                        .addContent(
                            Text.Builder(context, tempLo ?: WeatherIcons.PLACEHOLDER)
                                .setTypography(Typography.TYPOGRAPHY_BODY2)
                                .setColor(ColorProp.Builder(0xBFFFFFFF.toInt()).build())
                                .setMaxLines(1)
                                .build()
                        )
                        .build()
                )
            }
        }
        .build()
}

internal fun forecastItemLayout(
    context: Context,
    forecast: ForecastTileModel
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
        Text.Builder(context, forecast.hiTemp)
            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
            .setColor(ColorProp.Builder(Colors.WHITE).build())
            .setMaxLines(1)
            .setWeight(FONT_WEIGHT_NORMAL)
            .build()
    )
    .addContent(
        Text.Builder(context, forecast.loTemp)
            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
            .setColor(ColorProp.Builder(0xBFFFFFFF.toInt()).build())
            .setMaxLines(1)
            .setWeight(FONT_WEIGHT_NORMAL)
            .build()
    )
    .build()

internal class ForecastTileModel(context: Context, locale: Locale, forecast: Forecast) {
    private val isFahrenheit = Units.FAHRENHEIT == SettingsManager(context).getTemperatureUnit()
    val date: String = forecast.date.format(
        DateTimeFormatter.ofPattern(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK, locale)
    )
    val icon: String = forecast.icon
    val hiTemp: String = runCatching {
        val value = if (isFahrenheit) forecast.highF.roundToInt() else forecast.highC.roundToInt()
        String.format(locale, "%d°", value)
    }.getOrElse {
        WeatherIcons.PLACEHOLDER
    }
    val loTemp: String = runCatching {
        val value = if (isFahrenheit) forecast.lowF.roundToInt() else forecast.lowC.roundToInt()
        String.format(locale, "%d°", value)
    }.getOrElse {
        WeatherIcons.PLACEHOLDER
    }
}

@WearPreviewDevices
private fun forecastWeatherTilePreview(context: Context): TilePreviewData {
    val forecasts = MutableList(4) {
        ForecastTileModel(context, Locale.getDefault(), Forecast().apply {
            date = LocalDateTime.now().plusDays(it.toLong())
            highF = 70f
            highC = ConversionMethods.FtoC(70f)
            lowF = 65f
            lowC = ConversionMethods.FtoC(65f)
            icon = WeatherIcons.CLOUDY
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
                forecastWeatherTileLayout(
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