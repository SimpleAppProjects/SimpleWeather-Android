package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.weatherModule
import kotlin.math.roundToInt
import com.thewizrd.shared_resources.R as sharedRes

class CurrentLocationFeelsLikeWeatherComplicationService : WeatherForecastComplicationService() {
    companion object {
        private const val TAG = "CurrentFeelsLikeWeatherComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val complicationIcon = WeatherIcons.DAY_SUNNY
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
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("70°").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon).build(),
                        )
                    }
                }.build()
            }

            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Feels like: 75°").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setTitle(
                    PlainComplicationText.Builder("70°").build()
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
                }.build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(monochromaticIcon)
                        .build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).build()
            }

            else -> {
                null
            }
        }
    }

    override fun buildUpdate(
        dataType: ComplicationType,
        weather: Weather?,
        forecast: Forecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()

        // Temperature
        val currTemp =
            if (weather.condition?.tempF != null && weather.condition!!.tempF != weather.condition!!.tempC) {
                val temp =
                    if (isFahrenheit) Math.round(weather.condition!!.tempF) else Math.round(weather.condition!!.tempC)
                String.format(LocaleUtils.getLocale(), "%d", temp)
            } else {
                WeatherIcons.PLACEHOLDER
            }
        val feelsLike =
            if (weather.condition?.feelslikeF != null && weather.condition!!.feelslikeF != weather.condition!!.feelslikeC) {
                val temp =
                    if (isFahrenheit) weather.condition!!.feelslikeF.roundToInt() else weather.condition!!.feelslikeC.roundToInt()
                String.format(LocaleUtils.getLocale(), "%d°", temp)
            } else {
                WeatherIcons.EM_DASH
            }
        val feelsLikeLabel = getString(sharedRes.string.label_feelslike)

        val tempUnit = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS

        val temp = String.format(LocaleUtils.getLocale(), "%s°%s", currTemp, tempUnit)

        // Condition text
        val provider = weatherModule.weatherManager.getWeatherProvider(weather.source)
        val condition = if (provider.supportsWeatherLocale()) {
            weather.condition!!.weather
        } else {
            provider.getWeatherCondition(weather.condition!!.icon)
        }

        val wim = sharedDeps.weatherIconsManager
        val weatherIcon = wim.getWeatherIconResource(weather.condition!!.icon)
        val monochromaticIcon = Icon.createWithResource(this, weatherIcon)
            .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                weatherIcon
            )
        )

        val contentDescription =
            PlainComplicationText.Builder("$temp - $condition; $feelsLikeLabel - $feelsLike")
                .build()

        when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(temp).build(),
                    contentDescription
                )

                builder.setMonochromaticImage(
                    MonochromaticImage.Builder(icon).apply {
                        // Weather Icon
                        if (!wim.isFontIcon) {
                            setAmbientImage(monochromaticIcon)
                        }
                    }
                        .build()
                )

                if (!wim.isFontIcon) {
                    builder.setSmallImage(
                        SmallImage.Builder(icon, SmallImageType.ICON)
                            .setAmbientImage(monochromaticIcon)
                            .build()
                    )
                }

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }

            ComplicationType.LONG_TEXT -> {
                val builder = LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("$feelsLikeLabel - $feelsLike").build(),
                    contentDescription
                ).setTitle(
                    PlainComplicationText.Builder(temp).build()
                )

                // Weather Icon
                if (wim.isFontIcon) {
                    builder.setMonochromaticImage(
                        MonochromaticImage.Builder(icon).build()
                    )
                } else {
                    builder.setSmallImage(
                        SmallImage.Builder(icon, SmallImageType.ICON)
                            .setAmbientImage(monochromaticIcon)
                            .build()
                    )
                }

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                return MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    contentDescription
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                return SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(monochromaticIcon)
                        .build(),
                    contentDescription
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }

            else -> {
                return null
            }
        }
    }
}