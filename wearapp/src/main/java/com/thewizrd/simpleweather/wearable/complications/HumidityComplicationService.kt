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
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.R as sharedRes

class HumidityComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "HumidityComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )

    private val complicationIcon = WeatherIcons.HUMIDITY

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val monochromaticIcon =
            Icon.createWithResource(this, wip.getWeatherIconResource(complicationIcon))
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
                    75f, 0f, 100f,
                    PlainComplicationText.Builder("${getString(sharedRes.string.label_humidity)}: 75%")
                        .build()
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
                    PlainComplicationText.Builder("75%").build()
                ).setValueType(
                    RangedValueComplicationData.TYPE_PERCENTAGE
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("75%").build(),
                    PlainComplicationText.Builder("${getString(sharedRes.string.label_humidity)}: 75%")
                        .build()
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_humidity))
                        .build(),
                    PlainComplicationText.Builder("${getString(sharedRes.string.label_humidity)}: 75%")
                        .build()
                ).setTitle(
                    PlainComplicationText.Builder("75%").build()
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
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val humidityPct = weather.atmosphere?.humidity ?: hourlyForecast?.extras?.humidity
        val humidityStr = humidityPct?.let { "${it}%" } ?: WeatherIcons.EM_DASH

        val monochromaticIcon =
            Icon.createWithResource(this, wip.getWeatherIconResource(complicationIcon))
                .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                wim.getWeatherIconResource(complicationIcon)
            )
        )

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    humidityPct?.toFloat() ?: 0f, 0f, 100f,
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_humidity)}: $humidityStr"
                    ).build()
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
                    PlainComplicationText.Builder(humidityStr).build()
                ).setValueType(
                    RangedValueComplicationData.TYPE_PERCENTAGE
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(humidityStr).build(),
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_humidity)}: $humidityStr"
                    ).build()
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
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(sharedRes.string.label_humidity))
                        .build(),
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_humidity)}: $humidityStr"
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(humidityStr).build()
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