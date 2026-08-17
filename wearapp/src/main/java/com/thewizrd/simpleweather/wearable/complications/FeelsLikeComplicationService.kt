package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageType
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlin.math.roundToInt
import com.thewizrd.shared_resources.R as sharedRes

class FeelsLikeComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "FeelsLikeComplicationService"
    }

    override val supportedComplicationTypes =
        setOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)

    private val complicationIcon = WeatherIcons.THERMOMETER

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
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("75°").build(),
                    PlainComplicationText.Builder("Feels like: 75°").build()
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_feelslike))
                        .build(),
                    PlainComplicationText.Builder("Feels like: 75°").build()
                ).setTitle(
                    PlainComplicationText.Builder("75°").build()
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

        val feelsLikeF = weather.condition?.feelslikeF ?: hourlyForecast?.extras?.feelslikeF
        val feelsLikeC = weather.condition?.feelslikeC ?: hourlyForecast?.extras?.feelslikeC

        val tempUnit = settingsManager.getTemperatureUnit()
        val tempStr = if (feelsLikeF == null || feelsLikeC == null || feelsLikeF == feelsLikeC) {
            WeatherIcons.EM_DASH
        } else {
            val tempVal =
                if (tempUnit == Units.FAHRENHEIT) feelsLikeF.roundToInt() else feelsLikeC.toInt()
            String.format("$tempVal°$tempUnit")
        }

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
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(tempStr).build(),
                    PlainComplicationText.Builder(
                        String.format(
                            "%s: %s",
                            getString(sharedRes.string.label_feelslike),
                            tempStr
                        )
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_feelslike))
                        .build(),
                    PlainComplicationText.Builder(
                        String.format(
                            "%s: %s",
                            getString(sharedRes.string.label_feelslike),
                            tempStr
                        )
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(tempStr).build()
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