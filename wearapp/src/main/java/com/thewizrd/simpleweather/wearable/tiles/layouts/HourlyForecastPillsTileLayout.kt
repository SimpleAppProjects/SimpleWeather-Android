package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DeviceParametersBuilders.SCREEN_SHAPE_ROUND
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.StateBuilders.State
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.DataCardStyle
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.TitleContentPlacementInDataCard
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconDataCard
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
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

internal fun hourlyForecastPillsTileLayout(
    weather: Weather?,
    context: Context,
    requestParams: RequestBuilders.TileRequest
): LayoutElementBuilders.LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.hrForecast?.map { HourlyForecastTileModel(context, LocaleUtils.getLocale(), it) }

    return hourlyForecastPillsTileLayout(
        context,
        requestParams.deviceConfiguration,
        requestParams.currentState,
        location = viewModel?.location ?: WeatherIcons.EM_DASH,
        forecasts = forecasts
    )
}

internal fun hourlyForecastPillsTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    currentState: State,
    location: String,
    forecasts: List<HourlyForecastTileModel>?
): LayoutElementBuilders.LayoutElement {
    val lastClickedId = currentState.lastClickableId.toIntOrNull() ?: 0
    val iconSize = if (deviceParameters.isLargeHeight()) {
        dp(32f)
    } else {
        dp(26f)
    }

    return materialScope(context, deviceParameters, defaultColorScheme = wearTileColorScheme) {
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
                if (forecasts.isNullOrEmpty()) {
                    text(
                        text = context.getString(sharedRes.string.label_nodata).layoutString,
                        alignment = TEXT_ALIGN_CENTER
                    )
                } else {
                    buttonGroup(
                        height = if (deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) {
                            dp(deviceConfiguration.screenHeightDp * 0.49f)
                        } else {
                            expand()
                        }
                    ) {
                        forecasts.take(3).forEachIndexed { index, model ->
                            buttonGroupItem {
                                iconDataCard(
                                    onClick = clickable(id = index.toString()),
                                    width = weight(1f),
                                    height = expand(),
                                    titleContentPlacement = TitleContentPlacementInDataCard.Bottom,
                                    secondaryIcon = {
                                        icon(
                                            "${ID_WEATHER_ICON_PREFIX}${model.icon ?: WeatherIcons.NA}",
                                            width = iconSize,
                                            height = iconSize
                                        )
                                    },
                                    title = {
                                        text(
                                            text = model.temp.layoutString,
                                            alignment = TEXT_ALIGN_CENTER,
                                            maxLines = 1
                                        )
                                    },
                                    content = {
                                        text(
                                            text = model.date.layoutString,
                                            alignment = TEXT_ALIGN_CENTER,
                                            maxLines = 1
                                        )
                                    },
                                    shape = shapes.full,
                                    style = if (deviceConfiguration.isSmallWatch()) {
                                        DataCardStyle.defaultDataCardStyle()
                                    } else {
                                        DataCardStyle.largeDataCardStyle()
                                    },
                                    colors = if (index == lastClickedId) {
                                        filledVariantCardColors()
                                    } else {
                                        filledTonalCardColors()
                                    }
                                )
                            }
                        }
                    }
                }
            },
            bottomSlot = forecasts?.getOrNull(lastClickedId)?.let { model ->
                {
                    text(
                        modifier = LayoutModifier.padding(
                            horizontal = if (deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) {
                                4f
                            } else {
                                0f
                            },
                            vertical = if ((deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) && deviceConfiguration.isLargeHeight()) {
                                4f
                            } else {
                                0f
                            }
                        ),
                        text = model.condition.layoutString,
                        maxLines = if (deviceConfiguration.isLargeHeight()) {
                            2
                        } else {
                            1
                        }
                    )
                }
            }
        )
    }
}

@WearPreviewDevices
private fun hourlyForecastPillsTileLayout(context: Context): TilePreviewData {
    val forecasts = MutableList(3) {
        HourlyForecastTileModel(context, Locale.getDefault(), HourlyForecast().apply {
            date = ZonedDateTime.now().plusHours(it.toLong())
            highF = 70f
            highC = ConversionMethods.FtoC(70f)
            icon = WeatherIcons.DAY_PARTLY_CLOUDY
            condition = "Partly Cloudy"
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
                hourlyForecastPillsTileLayout(
                    context,
                    request.deviceConfiguration,
                    currentState = State.Builder().build(),
                    location = "New York",
                    forecasts = forecasts
                )
            ).build()
        }
    )
}