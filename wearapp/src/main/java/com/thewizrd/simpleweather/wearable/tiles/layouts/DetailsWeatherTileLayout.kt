package com.thewizrd.simpleweather.wearable.tiles.layouts

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastCoerceIn
import androidx.core.graphics.ColorUtils
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DeviceParametersBuilders.SCREEN_SHAPE_ROUND
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ModifiersBuilders.Transformation
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.compactButton
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.common.controls.BeaufortViewModel
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.PollenViewModel
import com.thewizrd.common.controls.UVIndexViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.common.utils.ImageUtils.rotate
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.designer.isInEditMode
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Astronomy
import com.thewizrd.shared_resources.weatherdata.model.Atmosphere
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.Condition
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.ForecastExtras
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Location
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase
import com.thewizrd.shared_resources.weatherdata.model.Pollen
import com.thewizrd.shared_resources.weatherdata.model.Precipitation
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.DetailsWeatherTileUtils
import com.thewizrd.simpleweather.ui.theme.wearTileColorScheme
import com.thewizrd.simpleweather.ui.tiles.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.ID_WEATHER_ICON_PREFIX
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

internal const val ID_DETAIL_ICON_PREFIX = "detail_icon:"
internal const val ID_ROTATION_PREFIX = "rotation:"

internal fun detailsWeatherTileLayout(
    weather: Weather?,
    weatherDetails: Map<WeatherDetailsType, DetailItemViewModel>? = null,
    context: Context,
    deviceParameters: DeviceParameters
): LayoutElement {
    return detailsWeatherTileLayout(
        context,
        deviceParameters,
        weather,
        weatherDetails ?: emptyMap()
    )
}

internal fun detailsWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    weather: Weather?,
    weatherDetails: Map<WeatherDetailsType, DetailItemViewModel> = emptyMap()
): LayoutElement {
    val tileConfig =
        DetailsWeatherTileUtils.getTileConfig() ?: DetailsWeatherTileUtils.DEFAULT_ITEMS
    val filteredDetails = weatherDetails.toList()
        .filter { tileConfig.contains(it.first) }
        .sortedBy { tileConfig.indexOf(it.first) }

    return detailsWeatherTileLayout(
        context,
        deviceParameters,
        weather,
        filteredDetails
    )
}

internal fun detailsWeatherTileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    weather: Weather?,
    filteredTileConfig: List<Pair<WeatherDetailsType, DetailItemViewModel>>
): LayoutElement {
    val detailItems = filteredTileConfig.take(DetailsWeatherTileUtils.MAX_BUTTONS)

    return materialScope(context, deviceParameters, defaultColorScheme = wearTileColorScheme) {
        Box.Builder()
            .setHeight(expand())
            .setWidth(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(
                when (detailItems.size) {
                    0 -> {
                        text(
                            text = context.getString(sharedRes.string.label_nodata).layoutString,
                            typography = androidx.wear.protolayout.material3.Typography.LABEL_LARGE
                        )
                    }

                    1, 2 -> {
                        primaryLayout(
                            titleSlot = if (deviceConfiguration.isLargeHeight()) {
                                weather?.location?.name?.let {
                                    {
                                        detailLocation(it)
                                    }
                                }
                            } else {
                                null
                            },
                            mainSlot = {
                                Column.Builder()
                                    .setWidth(expand())
                                    .apply {
                                        detailItems.forEachIndexed { index, (type, model) ->
                                            addContent(
                                                detailChipItem(
                                                    weather, type, model
                                                )
                                            )
                                            if (index != detailItems.size - 1) {
                                                addContent(
                                                    Spacer.Builder().setHeight(dp(4f)).build()
                                                )
                                            }
                                        }
                                    }
                                    .build()
                            },
                            bottomSlot = {
                                if (deviceConfiguration.screenShape == SCREEN_SHAPE_ROUND || deviceConfiguration.squareNotSupported()) {
                                    textEdgeButton(
                                        onClick = clickable(getLaunchAction(context)),
                                        labelContent = {
                                            text(context.getString(sharedRes.string.label_nav_weathernow).layoutString)
                                        }
                                    )
                                } else {
                                    compactButton(
                                        onClick = clickable(getLaunchAction(context)),
                                        labelContent = {
                                            text(context.getString(sharedRes.string.label_nav_weathernow).layoutString)
                                        }
                                    )
                                }
                            }
                        )
                    }

                    3, 4 -> {
                        Column.Builder()
                            .setHeight(wrap())
                            .setWidth(wrap())
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                            .setModifiers(
                                Modifiers.Builder()
                                    .setPadding(
                                        Padding.Builder()
                                            .setStart(dp(4f))
                                            .setEnd(dp(4f))
                                            .build()
                                    )
                                    .build()
                            )
                            .apply {
                                detailItems.chunked(2)
                                    .forEachIndexed { index, list ->
                                        addContent(
                                            Row.Builder()
                                                .setWidth(wrap())
                                                .setHeight(wrap())
                                                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                                .apply {
                                                    list.forEachIndexed { index, (type, model) ->
                                                        addContent(
                                                            Column.Builder()
                                                                .setWidth(dp(66f))
                                                                .setHeight(dp(66f))
                                                                .setHorizontalAlignment(
                                                                    HORIZONTAL_ALIGN_CENTER
                                                                )
                                                                .addContent(
                                                                    detailButtonItem(
                                                                        context,
                                                                        weather,
                                                                        type,
                                                                        model,
                                                                        buttonSize = dp(66f),
                                                                        imageSize = dp(24f),
                                                                        spacerSize = dp(10f),
                                                                        typography = Typography.TYPOGRAPHY_CAPTION2
                                                                    )
                                                                )
                                                                .build()
                                                        )
                                                        if (index != list.size - 1) {
                                                            addContent(
                                                                Spacer.Builder().setWidth(dp(4f))
                                                                    .build()
                                                            )
                                                        }
                                                    }
                                                }
                                                .build()
                                        )

                                        if (index != detailItems.size - 1) {
                                            addContent(Spacer.Builder().setHeight(dp(4f)).build())
                                        }
                                    }
                            }
                            .build()
                    }

                    else -> {
                        MultiButtonLayout.Builder()
                            .apply {
                                detailItems.forEach { (type, model) ->
                                    addButtonContent(
                                        detailButtonItem(
                                            context,
                                            weather,
                                            type,
                                            model
                                        )
                                    )
                                }
                            }
                            .build()
                    }
                }
            )
            .build()
    }
}

@OptIn(ProtoLayoutExperimental::class)
internal fun detailLocation(
    context: Context,
    location: String
): LayoutElement {
    return Text.Builder(context, location.split(',').firstOrNull() ?: location)
        .setTypography(Typography.TYPOGRAPHY_BODY1)
        .setColor(ColorProp.Builder(Colors.WHITE).build())
        .setOverflow(TEXT_OVERFLOW_MARQUEE)
        .setMaxLines(1)
        .build()
}

@OptIn(ProtoLayoutExperimental::class)
internal fun MaterialScope.detailLocation(
    location: String
): LayoutElement {
    return text(text = (location.split(',').firstOrNull() ?: location).layoutString)
}

internal fun MaterialScope.detailButtonItem(
    context: Context,
    weather: Weather?,
    detailsType: WeatherDetailsType,
    model: DetailItemViewModel,
    buttonSize: DpProp = dp(52f),
    imageSize: DpProp = dp(18f),
    spacerSize: DpProp = dp(8f),
    typography: Int? = null
): LayoutElement {
    return Box.Builder()
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .setHeight(expand())
        .setWidth(expand())
        .addContent(
            Column.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .setHeight(buttonSize)
                .setWidth(buttonSize)
                .setModifiers(
                    Modifiers.Builder()
                        .setBackground(
                            Background.Builder()
                                .setCorner(
                                    Corner.Builder()
                                        .setRadius(dp(buttonSize.value * 13f / 8))
                                        .build()
                                )
                                .setColor(
                                    ColorProp.Builder(colorScheme.surfaceContainerLow.staticArgb)
                                        .apply {
                                            colorScheme.surfaceContainer.dynamicArgb?.let {
                                                setDynamicValue(it)
                                            }
                                        }
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    Spacer.Builder().setHeight(spacerSize).build()
                )
                .addContent(
                    Image.Builder()
                        .setWidth(imageSize)
                        .setHeight(imageSize)
                        .setModifiers(
                            Modifiers.Builder()
                                .apply {
                                    if (deviceConfiguration.supportsTransformation()) {
                                        setTransformation(
                                            Transformation.Builder()
                                                .setRotation(degrees(model.iconRotation.toFloat()))
                                                .build()
                                        )
                                    }
                                }
                                .build()
                        )
                        .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
                        .setResourceId(
                            if (!deviceConfiguration.supportsTransformation() && model.iconRotation != 0) {
                                "${ID_ROTATION_PREFIX}${model.iconRotation}:${ID_WEATHER_ICON_PREFIX}${model.icon}"
                            } else {
                                "${ID_WEATHER_ICON_PREFIX}${model.icon}"
                            }
                        )
                        .build()
                )
                .addContent(
                    Spacer.Builder().setHeight(dp(3f)).build()
                )
                .addContent(
                    Text.Builder(context, model.shortValue.toString())
                        .setTypography(
                            typography ?: when (detailsType) {
                                WeatherDetailsType.FEELSLIKE,
                                WeatherDetailsType.HUMIDITY,
                                WeatherDetailsType.POPCLOUDINESS,
                                WeatherDetailsType.POPCHANCE,
                                WeatherDetailsType.DEWPOINT,
                                WeatherDetailsType.BEAUFORT,
                                WeatherDetailsType.UV,
                                WeatherDetailsType.AIRQUALITY -> Typography.TYPOGRAPHY_CAPTION2

                                else -> Typography.TYPOGRAPHY_CAPTION3
                            }
                        )
                        .setWeight(FONT_WEIGHT_NORMAL)
                        .setColor(
                            ColorProp.Builder(colorScheme.onSurface.staticArgb)
                                .apply {
                                    colorScheme.onSurface.dynamicArgb?.let {
                                        setDynamicValue(it)
                                    }
                                }
                                .build()
                        )
                        .setMaxLines(1)
                        .setScalable(false)
                        .setModifiers(
                            Modifiers.Builder()
                                .setSemantics(
                                    Semantics.Builder()
                                        .setRole(ModifiersBuilders.SEMANTICS_ROLE_BUTTON)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .apply {
            when (detailsType) {
                WeatherDetailsType.SUNRISE,
                WeatherDetailsType.SUNSET,
                WeatherDetailsType.FEELSLIKE,
                WeatherDetailsType.WINDSPEED,
                WeatherDetailsType.WINDGUST,
                WeatherDetailsType.VISIBILITY,
                WeatherDetailsType.POPRAIN,
                WeatherDetailsType.POPSNOW,
                WeatherDetailsType.DEWPOINT,
                WeatherDetailsType.MOONRISE,
                WeatherDetailsType.MOONSET,
                WeatherDetailsType.MOONPHASE -> {
                }

                WeatherDetailsType.UV -> {
                    weather?.condition?.uv?.let {
                        val uvModel = UVIndexViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    uvModel.progress.div(uvModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        uvModel.progressColor,
                                        ColorUtils.blendARGB(
                                            uvModel.progressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }

                WeatherDetailsType.AIRQUALITY -> {
                    weather?.condition?.airQuality?.let {
                        val aqiModel = AirQualityViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    aqiModel.progress.div(aqiModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        aqiModel.progressColor,
                                        ColorUtils.blendARGB(
                                            aqiModel.progressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }

                WeatherDetailsType.BEAUFORT -> {
                    weather?.condition?.beaufort?.let {
                        val beaufortModel = BeaufortViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    beaufortModel.progress.div(beaufortModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        beaufortModel.progressColor,
                                        ColorUtils.blendARGB(
                                            beaufortModel.progressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }

                WeatherDetailsType.HUMIDITY -> {
                    weather?.atmosphere?.humidity?.let {
                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(it.div(100f).fastCoerceIn(0f, 1f))
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .build()
                        )
                    }
                }

                WeatherDetailsType.POPCLOUDINESS -> {
                    weather?.precipitation?.cloudiness?.let {
                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(it.div(100f).fastCoerceIn(0f, 1f))
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .build()
                        )
                    }
                }

                WeatherDetailsType.POPCHANCE -> {
                    weather?.precipitation?.pop?.let {
                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(it.div(100f).fastCoerceIn(0f, 1f))
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .build()
                        )
                    }
                }

                WeatherDetailsType.PRESSURE -> {
                    weather?.atmosphere?.pressureIn?.let {
                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(((it - 26f) / (32f - 26f)).fastCoerceIn(0f, 1f))
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .build()
                        )
                    }
                }

                WeatherDetailsType.TREEPOLLEN -> {
                    weather?.condition?.pollen?.let {
                        val pollenModel = PollenViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    pollenModel.treePollenProgress.div(pollenModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        pollenModel.treePollenProgressColor,
                                        ColorUtils.blendARGB(
                                            pollenModel.treePollenProgressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }

                WeatherDetailsType.GRASSPOLLEN -> {
                    weather?.condition?.pollen?.let {
                        val pollenModel = PollenViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    pollenModel.grassPollenProgress.div(pollenModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        pollenModel.grassPollenProgressColor,
                                        ColorUtils.blendARGB(
                                            pollenModel.grassPollenProgressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }

                WeatherDetailsType.RAGWEEDPOLLEN -> {
                    weather?.condition?.pollen?.let {
                        val pollenModel = PollenViewModel(it)

                        addContent(
                            CircularProgressIndicator.Builder()
                                .setProgress(
                                    pollenModel.ragweedPollenProgress.div(pollenModel.progressMax.toFloat())
                                        .fastCoerceIn(0f, 1f)
                                )
                                .setStrokeWidth(dp(2f))
                                .setOuterMarginApplied(false)
                                .setCircularProgressIndicatorColors(
                                    ProgressIndicatorColors(
                                        pollenModel.ragweedPollenProgressColor,
                                        ColorUtils.blendARGB(
                                            pollenModel.ragweedPollenProgressColor,
                                            colorScheme.surfaceContainerLow.staticArgb,
                                            0.95f
                                        )
                                    )
                                )
                                .build()
                        )
                    }
                }
            }
        }
        .build()
}

internal fun MaterialScope.detailChipItem(
    weather: Weather?,
    detailsType: WeatherDetailsType,
    model: DetailItemViewModel
): LayoutElement {
    return button(
        width = expand(),
        onClick = Clickable.Builder().build(),
        iconContent = {
            icon(
                if (appLib.isInEditMode() || deviceConfiguration.supportsTransformation() || model.iconRotation == 0) {
                    "${ID_WEATHER_ICON_PREFIX}${model.icon}"
                } else {
                    "${ID_ROTATION_PREFIX}${model.iconRotation}:${ID_WEATHER_ICON_PREFIX}${model.icon}"
                }
            )
        },
        labelContent = {
            text(
                text = model.label.toString().layoutString
            )
        },
        secondaryLabelContent = {
            text(
                text = model.value.toString().layoutString
            )
        },
        colors = filledTonalButtonColors()
    )
}

private fun getLaunchAction(context: Context): ActionBuilders.Action {
    return ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName(LaunchActivity::class.java.name)
                .build()
        )
        .build()
}

@WearPreviewDevices
private fun detailsWeatherTilePreview(context: Context): TilePreviewData {
    context.initializeDependencies(isPhone = false)

    val wim = sharedDeps.weatherIconsManager.iconProvider

    val weather = buildMockWeatherData()
    val viewModel = weather.toUiModel()
    val weatherDetails = viewModel.weatherDetailsMap
    val sampleTileConfig = weatherDetails
        .filterKeys { DetailsWeatherTileUtils.isTypeAllowed(it) }
        .toList()
        .shuffled()
        .take(Random.nextInt(1, DetailsWeatherTileUtils.MAX_BUTTONS + 1 /* exclusive */))
        .shuffled()

    return TilePreviewData(
        onTileResourceRequest = { request ->
            Resources.Builder()
                .apply {
                    weatherDetails.forEach { (_, model) ->
                        addIdToImageMapping(
                            if (!request.deviceConfiguration.supportsTransformation() && model.iconRotation != 0) {
                                "${ID_ROTATION_PREFIX}${model.iconRotation}:${ID_WEATHER_ICON_PREFIX}${model.icon}"
                            } else {
                                "${ID_WEATHER_ICON_PREFIX}${model.icon}"
                            },
                            ImageUtils.tintedBitmapFromDrawable(
                                context,
                                wim.getWeatherIconResource(model.icon),
                                Colors.WHITE
                            ).run {
                                if (request.deviceConfiguration.supportsTransformation()) {
                                    this
                                } else {
                                    rotate(model.iconRotation.toFloat())
                                }
                            }.toImageResource()
                        )
                    }
                }
                .build()
        },
        onTileRequest = { request ->
            TilePreviewHelper.singleTimelineEntryTileBuilder(
                detailsWeatherTileLayout(
                    context,
                    request.deviceConfiguration,
                    weather,
                    sampleTileConfig
                )
            ).build()
        }
    )
}

private fun buildMockWeatherData(): Weather {
    return Weather().apply {
        location = Location().apply {
            name = "Location"
            tzLong = "UTC"
        }
        updateTime = ZonedDateTime.now()
        forecast = List(6) { index ->
            Forecast().apply {
                date = LocalDateTime.now().plusDays(index.toLong())
                highF = 70f + index
                highC = 23f + index / 2f
                lowF = 60f - index
                lowC = 17f - index / 2f
                condition = "Sunny"
                icon = WeatherIcons.DAY_SUNNY
                extras = ForecastExtras().apply {
                    feelslikeF = 80f
                    feelslikeC = 26f
                    humidity = 50
                    dewpointF = 30f
                    dewpointC = -1f
                    uvIndex = 5f
                    pop = 35
                    cloudiness = 25
                    qpfRainIn = 0.05f
                    qpfRainMm = 1.27f
                    qpfSnowIn = 0f
                    qpfSnowCm = 0f
                    pressureIn = 30.05f
                    pressureMb = 1018f
                    windDegrees = 180
                    windMph = 4f
                    windKph = 6.43f
                    windGustKph = 9f
                    windGustKph = 14.5f
                    visibilityMi = 10f
                    visibilityKm = 16.1f
                }
            }
        }
        hrForecast = List(6) { index ->
            HourlyForecast().apply {
                date = ZonedDateTime.now().plusHours(index.toLong())
                highF = 70f + index
                highC = 23f + index / 2f
                condition = "Sunny"
                icon = WeatherIcons.DAY_SUNNY
                windMph = 5f
                windKph = 8f
                extras = ForecastExtras().apply {
                    feelslikeF = 80f
                    feelslikeC = 26f
                    humidity = 50
                    dewpointF = 30f
                    dewpointC = -1f
                    uvIndex = 5f
                    pop = 35
                    cloudiness = 25
                    qpfRainIn = 0.05f
                    qpfRainMm = 1.27f
                    qpfSnowIn = 0f
                    qpfSnowCm = 0f
                    pressureIn = 30.05f
                    pressureMb = 1018f
                    windDegrees = 180
                    windMph = 4f
                    windKph = 6.43f
                    windGustKph = 9f
                    windGustKph = 14.5f
                    visibilityMi = 10f
                    visibilityKm = 16.1f
                }
            }
        }
        minForecast = List(10) {
            val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).let {
                it.withMinute(it.minute - (it.minute % 10))
            }

            MinutelyForecast().apply {
                date = now.plusMinutes(it.toLong() * 10)
                rainMm = Random.nextFloat()
            }
        }
        aqiForecast = List(6) {
            AirQuality().apply {
                date = LocalDate.now().plusDays(it.toLong())
                index = 10 * (it)
            }
        }
        condition = Condition().apply {
            weather = "Sunny"
            tempF = 70f
            tempC = 21f
            windDegrees = 292
            windMph = 5f
            windKph = 8f
            windGustMph = 15f
            windGustKph = 25f
            feelslikeF = 75f
            feelslikeC = 23f
            highF = 75f
            highC = 23f
            lowF = 60f
            lowC = 15f
            icon = WeatherIcons.DAY_SUNNY
            airQuality = AirQuality().apply {
                index = Random.nextInt(0, 301)
            }
            beaufort = Beaufort(Beaufort.BeaufortScale.valueOf(Random.nextInt(0, 12)))
            uv = UV(Random.nextInt(0, 11).toFloat())
            pollen = Pollen().apply {
                treePollenCount = Pollen.PollenCount.VERY_HIGH
                grassPollenCount = Pollen.PollenCount.LOW
                ragweedPollenCount = Pollen.PollenCount.MODERATE
            }
        }
        atmosphere = Atmosphere().apply {
            humidity = 50
            pressureIn = 30.3f
            pressureMb = 1026.41f
            visibilityMi = 10f
            visibilityKm = 16f
            dewpointF = 50f
            dewpointC = 10f
        }
        precipitation = Precipitation().apply {
            pop = 100
            cloudiness = 100
            qpfRainIn = 0.05f
            qpfRainMm = 1.27f
            qpfSnowIn = 10.1f
            qpfSnowCm = 25.4f
        }
        astronomy = Astronomy().apply {
            sunrise = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0))
            sunset = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0))
            moonrise = LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 43))
            moonset = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 46))
            moonPhase = MoonPhase(MoonPhase.MoonPhaseType.entries[Random.nextInt(0, 7)])
        }
        source = WeatherAPI.ANDROID
        query = ""
    }
}