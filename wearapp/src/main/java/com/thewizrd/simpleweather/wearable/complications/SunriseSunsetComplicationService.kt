package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import android.text.format.DateFormat
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
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.LocalDateTime
import java.time.ZonedDateTime
import com.thewizrd.shared_resources.R as sharedRes

class SunriseSunsetComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "SunriseSunsetComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val complicationIcon = WeatherIcons.SUNSET
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
                    PlainComplicationText.Builder("6:05 PM").build(),
                    PlainComplicationText.Builder("Sunset: 6:05 PM").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
                                .build()
                        )
                    }
                }.build()
            }

            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Sunset").build(),
                    PlainComplicationText.Builder("Sunset: 6:05 PM").build()
                ).setTitle(
                    PlainComplicationText.Builder("6:05 PM").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
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

        val sunrise = weather.astronomy?.sunrise
        val sunset = weather.astronomy?.sunset

        val fmt = if (DateFormat.is24HourFormat(this)) {
            DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR)
        } else {
            DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM)
        }

        val tz = hourlyForecast?.date?.offset
        val now = if (tz != null) {
            ZonedDateTime.now(tz).toLocalDateTime()
        } else {
            LocalDateTime.now()
        }

        val text: String
        val complicationIcon: String
        val desc: String

        if (sunset != null && sunrise != null) {
            if (now.toLocalTime() > sunrise.toLocalTime()) {
                text = sunset.format(fmt)
                complicationIcon = WeatherIcons.SUNSET
                desc = getString(sharedRes.string.label_sunset)
            } else {
                text = sunrise.format(fmt)
                complicationIcon = WeatherIcons.SUNRISE
                desc = getString(sharedRes.string.label_sunrise)
            }
        } else if (sunset != null) {
            text = sunset.format(fmt)
            complicationIcon = WeatherIcons.SUNSET
            desc = getString(sharedRes.string.label_sunset)
        } else {
            text = sunrise?.format(fmt) ?: WeatherIcons.EM_DASH
            complicationIcon = WeatherIcons.SUNRISE
            desc = getString(sharedRes.string.label_sunrise)
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
                    PlainComplicationText.Builder(text).build(),
                    PlainComplicationText.Builder("$desc: $text").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
                                .build()
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }

            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(desc).build(),
                    PlainComplicationText.Builder("$desc: $text").build()
                ).setTitle(
                    PlainComplicationText.Builder(text).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
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