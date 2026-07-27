package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DeviceParametersBuilders.SCREEN_SHAPE_ROUND
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
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
import androidx.wear.protolayout.StateBuilders.State
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.PrimaryLayoutMargins
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
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
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.theme.wearTileColorScheme
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.ZonedDateTime
import java.util.Locale

internal fun hourlyForecastCardTileLayout(
    weather: Weather?,
    context: Context,
    requestParams: RequestBuilders.TileRequest
): LayoutElement {
    val viewModel = weather?.toUiModel()
    val forecasts =
        weather?.hrForecast?.map { HourlyForecastTileModel(context, LocaleUtils.getLocale(), it) }

    return hourlyForecastCardTileLayout(
        context,
        requestParams.deviceConfiguration,
        requestParams.currentState,
        location = viewModel?.location ?: WeatherIcons.EM_DASH,
        forecasts = forecasts
    )
}

internal fun hourlyForecastCardTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    currentState: State,
    location: String,
    forecasts: List<HourlyForecastTileModel>?
): LayoutElement {
    val lastClickedId = currentState.lastClickableId.toIntOrNull() ?: 0
    val forecastCount = if (deviceParameters.isLargeWidth()) 4 else 3
    val iconSize = if (deviceParameters.isLargeWatch()) {
        dp(32f)
    } else {
        dp(26f)
    }

    return materialScope(context, deviceParameters, defaultColorScheme = wearTileColorScheme) {
        primaryLayout(
            margins = if (deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) {
                if (deviceConfiguration.isLargeWidth()) {
                    PrimaryLayoutMargins.customizedPrimaryLayoutMargin(0.075f, 0.075f)
                } else {
                    PrimaryLayoutMargins.MID_PRIMARY_LAYOUT_MARGIN
                }
            } else {
                PrimaryLayoutMargins.MIN_PRIMARY_LAYOUT_MARGIN
            },
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
                    Row.Builder()
                        .setModifiers(
                            Modifiers.Builder()
                                .setBackground(
                                    Background.Builder()
                                        .setColor(
                                            ColorBuilders.ColorProp.Builder(filledTonalCardColors().backgroundColor.staticArgb)
                                                .apply {
                                                    filledTonalCardColors().backgroundColor.dynamicArgb?.let {
                                                        setDynamicValue(it)
                                                    }
                                                }
                                                .build()
                                        )
                                        .setCorner(shapes.large)
                                        .build()
                                )
                                .setPadding(padding(8f))
                                .build()
                        )
                        .setHeight(expand())
                        .setWidth(expand())
                        .addContent(
                            Column.Builder()
                                .setWidth(expand())
                                .setHeight(expand())
                                .addContent(
                                    buttonGroup(
                                        width = expand(),
                                        height = expand(),
                                        spacing = 0f
                                    ) {
                                        forecasts.take(forecastCount)
                                            .forEachIndexed { index, model ->
                                                buttonGroupItem {
                                                    Box.Builder()
                                                        .setHeight(expand())
                                                        .setWidth(expand())
                                                        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                                        .setHorizontalAlignment(
                                                            HORIZONTAL_ALIGN_CENTER
                                                        )
                                                        .setModifiers(
                                                            Modifiers.Builder()
                                                                .setClickable(
                                                                    clickable(id = index.toString())
                                                                )
                                                                .setBackground(
                                                                    Background.Builder()
                                                                        .setColor(
                                                                            filledTonalCardColors().let {
                                                                                if (lastClickedId == index) {
                                                                                    it.copy(
                                                                                        backgroundColor = this.colorScheme.surfaceContainerHigh
                                                                                    )
                                                                                } else {
                                                                                    it
                                                                                }
                                                                            }.backgroundColor.prop
                                                                        )
                                                                        .setCorner(shapes.medium)
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
                                                                                top = 8f,
                                                                                end = 4f,
                                                                                bottom = 8f
                                                                            )
                                                                        )
                                                                        .build()
                                                                )
                                                                .addContent(
                                                                    Box.Builder()
                                                                        .setHeight(expand())
                                                                        .setWidth(wrap())
                                                                        .setVerticalAlignment(
                                                                            VERTICAL_ALIGN_CENTER
                                                                        )
                                                                        .setHorizontalAlignment(
                                                                            HORIZONTAL_ALIGN_CENTER
                                                                        )
                                                                        .addContent(
                                                                            icon(
                                                                                "${ID_WEATHER_ICON_PREFIX}${model.icon ?: WeatherIcons.NA}",
                                                                                height = iconSize,
                                                                                width = iconSize
                                                                            )
                                                                        )
                                                                        .build()
                                                                )
                                                                .addContent(
                                                                    text(
                                                                        text = model.temp.removeSuffix(
                                                                            "°"
                                                                        ).layoutString,
                                                                        alignment = TEXT_ALIGN_CENTER,
                                                                        maxLines = 1,
                                                                        typography = Typography.TITLE_LARGE,
                                                                        color = colorScheme.onBackground,
                                                                        settings = listOf(
                                                                            FontSetting.weight(
                                                                                FONT_WEIGHT_NORMAL
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                                .addContent(
                                                                    Spacer.Builder()
                                                                        .setHeight(dp(4f)).build()
                                                                )
                                                                .addContent(
                                                                    text(
                                                                        text = model.date.layoutString,
                                                                        alignment = TEXT_ALIGN_CENTER,
                                                                        maxLines = 1,
                                                                        color = colorScheme.onSurface,
                                                                        typography = Typography.BODY_SMALL,
                                                                        settings = listOf(
                                                                            FontSetting.weight(
                                                                                FONT_WEIGHT_NORMAL
                                                                            )
                                                                        )
                                                                    )
                                                                )
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
                            vertical = 0f
                        ),
                        text = if (deviceConfiguration.isLargeHeight()) {
                            if (model.condition.contains(" "))
                                model.condition.replaceFirst(" ", StringUtils.lineSeparator())
                            else
                                model.condition + StringUtils.lineSeparator()
                        } else {
                            model.condition
                        }.layoutString,
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
private fun hourlyForecastCardTileLayout(context: Context): TilePreviewData {
    val forecasts = MutableList(4) {
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
                hourlyForecastCardTileLayout(
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