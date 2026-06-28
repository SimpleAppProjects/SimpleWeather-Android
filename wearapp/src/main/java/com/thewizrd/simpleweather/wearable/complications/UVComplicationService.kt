package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageType
import com.thewizrd.common.controls.UVIndexViewModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlin.math.max
import com.thewizrd.shared_resources.R as sharedRes

class UVComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "UVComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)
        val complicationIcon = WeatherIcons.UV_INDEX_3

        val monochromaticIcon =
            Icon.createWithResource(this, wim.getWeatherIconResource(complicationIcon))
                .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                wim.getWeatherIconResource(complicationIcon)
            )
        )

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 11f,
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.setText(
                    PlainComplicationText.Builder("3").build()
                ).setTitle(
                    PlainComplicationText.Builder("UV").build()
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("3").build(),
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(sharedRes.string.label_uv)).build(),
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                ).setTitle(
                    PlainComplicationText.Builder("3, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.build()
            }
            else -> {
                null
            }
        }
    }

    override fun buildUpdate(
        dataType: ComplicationType,
        weather: Weather?,
        hourlyForecast: HourlyForecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val wim = sharedDeps.weatherIconsManager

        val uvIndex = weather.condition?.uv?.index ?: hourlyForecast?.extras?.uvIndex
        val uvModel = uvIndex?.let { UVIndexViewModel(UV(it)) }
        val uvStr =
            uvModel?.let { "${uvModel.index}, ${uvModel.description}" } ?: WeatherIcons.EM_DASH
        val uvIdxStr = uvModel?.index?.toString() ?: WeatherIcons.EM_DASH
        val uvProgress = uvModel?.progress?.toFloat() ?: 0f
        val uvProgressMax = uvModel?.let { max(it.progressMax, it.progress).toFloat() } ?: 11f
        val contentDescription = "${getString(sharedRes.string.label_uv)}: $uvStr"
        val uvIcon = uvModel?.icon ?: WeatherIcons.UV_INDEX

        val monochromaticIcon = Icon.createWithResource(this, wim.getWeatherIconResource(uvIcon))
            .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                wim.getWeatherIconResource(uvIcon)
            )
        )

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    uvProgress, 0f, uvProgressMax,
                    PlainComplicationText.Builder(
                        contentDescription
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setText(
                    PlainComplicationText.Builder(uvIdxStr).build()
                ).setTitle(
                    PlainComplicationText.Builder("UV").build()
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(uvIdxStr).build(),
                    PlainComplicationText.Builder(
                        contentDescription
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(sharedRes.string.label_uv)).build(),
                    PlainComplicationText.Builder(
                        contentDescription
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(uvStr).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            else -> {
                null
            }
        }
    }
}