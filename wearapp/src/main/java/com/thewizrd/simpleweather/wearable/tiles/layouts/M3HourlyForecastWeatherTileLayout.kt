package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DeviceParametersBuilders.SCREEN_SHAPE_ROUND
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.FontSetting
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.theme.wearTileColorScheme
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.ZonedDateTime
import java.util.Locale

internal fun m3HourlyForecastWeatherTileLayout(
    weather: Weather?,
    context: Context,
    requestParams: RequestBuilders.TileRequest
): LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.hrForecast?.map { HourlyForecastTileModel(context, LocaleUtils.getLocale(), it) }

    return m3HourlyForecastWeatherTileLayout(
        context,
        requestParams.deviceConfiguration,
        requestParams.currentState,
        location = viewModel?.location ?: WeatherIcons.EM_DASH,
        weatherIconId = "$ID_WEATHER_ICON_PREFIX${viewModel?.weatherIcon ?: WeatherIcons.NA}",
        currentTemperature = viewModel?.curTemp?.replace(viewModel.tempUnit ?: "", "")
            ?: WeatherIcons.EM_DASH,
        tempHi = viewModel?.hiTemp,
        tempLo = viewModel?.loTemp,
        forecasts = forecasts
    )
}

private fun m3HourlyForecastWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    currentState: StateBuilders.State,
    location: String,
    weatherIconId: String,
    currentTemperature: String,
    tempHi: String? = null,
    tempLo: String? = null,
    forecasts: List<HourlyForecastTileModel>?
): LayoutElement =
    materialScope(context, deviceParameters, defaultColorScheme = wearTileColorScheme) {
    primaryLayout(
        margins = PrimaryLayoutMargins.customizedPrimaryLayoutMargin(
            start = 3f / 100,
            end = 3f / 100,
            bottom = 10f / 100
        ),
        titleSlot = {
            text(
                modifier = LayoutModifier.padding(horizontal = 4f, vertical = 0f),
                text = (location.split(',').firstOrNull() ?: location).layoutString
            )
        },
        mainSlot = {
            Column.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(
                    buttonGroup(
                        height = weight(1f),
                        width = expand()
                    ) {
                        buttonGroupItem {
                            Column.Builder()
                                .setWidth(expand())
                                .addContent(
                                    icon(
                                        weatherIconId,
                                        width = dp(32f),
                                        height = dp(32f)
                                    )
                                )
                                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                                .build()
                        }
                        buttonGroupItem {
                            Column.Builder()
                                .setWidth(
                                    if (tempHi != null || tempLo != null) {
                                        wrap()
                                    } else {
                                        expand()
                                    }
                                )
                                .addContent(
                                    text(
                                        text = currentTemperature.layoutString,
                                        typography = if (deviceConfiguration.isLargeWidth()) {
                                            Typography.NUMERAL_LARGE
                                        } else {
                                            Typography.NUMERAL_MEDIUM
                                        }
                                    )
                                )
                                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                                .build()
                        }
                        if (tempHi != null || tempLo != null) {
                            buttonGroupItem {
                                Column.Builder()
                                    .setWidth(expand())
                                    .setHeight(wrap())
                                    .addContent(
                                        text(
                                            (tempHi ?: WeatherIcons.PLACEHOLDER).layoutString,
                                            typography = Typography.LABEL_MEDIUM
                                        )
                                    )
                                    .addContent(
                                        text(
                                            (tempLo ?: WeatherIcons.PLACEHOLDER).layoutString,
                                            typography = Typography.LABEL_MEDIUM,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    )
                                    .build()
                            }
                        }
                    })
                .apply {
                    if (forecasts.isNullOrEmpty()) {
                        addContent(
                            text(
                                text = context.getString(sharedRes.string.label_nodata).layoutString,
                                alignment = TEXT_ALIGN_CENTER
                            )
                        )
                    } else {
                        addContent(
                            buttonGroup(
                                width = if (deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) {
                                    dp(deviceConfiguration.screenWidthDp / 1.4f)
                                } else {
                                    expand()
                                },
                                height = if (deviceConfiguration.isSmallHeight()) {
                                    weight(1.15f)
                                } else {
                                    weight(1.55f)
                                }
                            ) {
                                buttonGroupItem {
                                    Row.Builder()
                                        .setModifiers(
                                            Modifiers.Builder()
                                                .setBackground(
                                                    Background.Builder()
                                                        .setColor(
                                                            ColorProp.Builder(filledTonalCardColors().backgroundColor.staticArgb)
                                                                .apply {
                                                                    filledTonalCardColors().backgroundColor.dynamicArgb?.let {
                                                                        setDynamicValue(it)
                                                                    }
                                                                }
                                                                .build()
                                                        )
                                                        .setCorner(
                                                            if (deviceConfiguration.isSmallHeight()) {
                                                                shapes.full
                                                            } else {
                                                                shapes.large
                                                            }
                                                        )
                                                        .build()
                                                )
                                                .setPadding(padding(6f))
                                                .build()
                                        )
                                        .setHeight(expand())
                                        .setWidth(expand())
                                        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                        .addContent(
                                            Column.Builder()
                                                .setWidth(expand())
                                                .setHeight(expand())
                                                .addContent(
                                                    buttonGroup(
                                                        width = weight(1f),
                                                        height = weight(1f),
                                                        spacing = 0f
                                                    ) {
                                                        forecasts.take(3)
                                                            .forEachIndexed { index, model ->
                                                                buttonGroupItem {
                                                                    Box.Builder()
                                                                        .setHeight(expand())
                                                                        .setWidth(weight(1f))
                                                                        .setVerticalAlignment(
                                                                            VERTICAL_ALIGN_CENTER
                                                                        )
                                                                        .setHorizontalAlignment(
                                                                            HORIZONTAL_ALIGN_CENTER
                                                                        )
                                                                        .setModifiers(
                                                                            Modifiers.Builder()
                                                                                .setBackground(
                                                                                    Background.Builder()
                                                                                        .setColor(
                                                                                            filledTonalCardColors().backgroundColor.prop
                                                                                        )
                                                                                        .setCorner(
                                                                                            if (deviceConfiguration.isSmallHeight()) {
                                                                                                shapes.large
                                                                                            } else {
                                                                                                shapes.medium
                                                                                            }
                                                                                        )
                                                                                        .build()
                                                                                )
                                                                                .build()
                                                                        )
                                                                        .addContent(
                                                                            Column.Builder()
                                                                                .setHeight(expand())
                                                                                .setWidth(expand())
                                                                                .setModifiers(
                                                                                    Modifiers.Builder()
                                                                                        .setPadding(
                                                                                            padding(
                                                                                                start = 4f,
                                                                                                top = 4f,
                                                                                                end = 4f,
                                                                                                bottom = 8f
                                                                                            )
                                                                                        )
                                                                                        .build()
                                                                                )
                                                                                .addContent(
                                                                                    Box.Builder()
                                                                                        .setHeight(
                                                                                            expand()
                                                                                        )
                                                                                        .setWidth(
                                                                                            wrap()
                                                                                        )
                                                                                        .setVerticalAlignment(
                                                                                            VERTICAL_ALIGN_CENTER
                                                                                        )
                                                                                        .setHorizontalAlignment(
                                                                                            HORIZONTAL_ALIGN_CENTER
                                                                                        )
                                                                                        .addContent(
                                                                                            icon(
                                                                                                "${ID_WEATHER_ICON_PREFIX}${model.icon ?: WeatherIcons.NA}",
                                                                                            )
                                                                                        )
                                                                                        .build()
                                                                                )
                                                                                .addContent(
                                                                                    text(
                                                                                        text = if (deviceConfiguration.isSmallHeight()) {
                                                                                            model.date.layoutString
                                                                                        } else {
                                                                                            model.temp.removeSuffix(
                                                                                                "°"
                                                                                            ).layoutString
                                                                                        },
                                                                                        alignment = TEXT_ALIGN_CENTER,
                                                                                        maxLines = 1,
                                                                                        typography = if (deviceConfiguration.isSmallHeight()) {
                                                                                            Typography.BODY_EXTRA_SMALL
                                                                                        } else {
                                                                                            Typography.TITLE_MEDIUM
                                                                                        },
                                                                                        color = if (deviceConfiguration.isSmallHeight()) {
                                                                                            colorScheme.onSurface
                                                                                        } else {
                                                                                            colorScheme.onBackground
                                                                                        },
                                                                                        settings = if (deviceConfiguration.isSmallHeight()) {
                                                                                            listOf(
                                                                                                FontSetting.weight(
                                                                                                    FONT_WEIGHT_NORMAL
                                                                                                )
                                                                                            )
                                                                                        } else {
                                                                                            emptyList()
                                                                                        }
                                                                                    )
                                                                                )
                                                                                .apply {
                                                                                    if (!deviceConfiguration.isSmallHeight()) {
                                                                                        addContent(
                                                                                            Spacer.Builder()
                                                                                                .setHeight(
                                                                                                    dp(2f)
                                                                                                )
                                                                                                .build()
                                                                                        )
                                                                                        addContent(
                                                                                            text(
                                                                                                text = model.date.layoutString,
                                                                                                alignment = TEXT_ALIGN_CENTER,
                                                                                                maxLines = 1,
                                                                                                color = colorScheme.primary,
                                                                                                typography = Typography.BODY_EXTRA_SMALL,
                                                                                                settings = listOf(
                                                                                                    FontSetting.weight(
                                                                                                        FONT_WEIGHT_NORMAL
                                                                                                    )
                                                                                                )
                                                                                            )
                                                                                        )
                                                                                    }
                                                                                }
                                                                                .build()
                                                                        )
                                                                        .build()
                                                                }
                                                            }
                                                    }
                                                )
                                                .build()
                                        )
                                        .build()
                                }
                            }
                        )
                    }
                }
                .build()
        }
    )
}

@WearPreviewDevices
private fun hourlyForecastWeatherTilePreview(context: Context): TilePreviewData {
    val forecasts = MutableList(3) {
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
                m3HourlyForecastWeatherTileLayout(
                    context,
                    request.deviceConfiguration,
                    request.currentState,
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