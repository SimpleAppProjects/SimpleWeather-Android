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
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getWindDirection
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlin.math.roundToInt
import com.thewizrd.shared_resources.R as sharedRes

class WindComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "WindComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

    private val complicationIcon = WeatherIcons.WIND_DIRECTION

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val icon = Icon.createWithBitmap(
            ImageUtils.rotateBitmap(
                ImageUtils.bitmapFromDrawable(
                    getThemeContextOverride(false),
                    wim.getWeatherIconResource(complicationIcon)
                ), 330.0f // 150° + 180
            )
        )
        val monochromaticIcon = Icon.createWithBitmap(
            ImageUtils.rotateBitmap(
                ImageUtils.bitmapFromDrawable(
                    getThemeContextOverride(false),
                    wip.getWeatherIconResource(complicationIcon)
                ), 330.0f // 150° + 180
            )
        )
            .setTint(Colors.WHITESMOKE)

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("5 mph").build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
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
                    PlainComplicationText.Builder("Wind").build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
                ).setTitle(
                    PlainComplicationText.Builder("5 mph, SSE").build()
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

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(icon)
                        .build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
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
        hourlyForecast: HourlyForecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val windMph = weather.condition?.windMph ?: hourlyForecast?.extras?.windMph
        val windKph = weather.condition?.windKph ?: hourlyForecast?.extras?.windKph
        val windDirection = weather.condition?.windDegrees ?: hourlyForecast?.extras?.windDegrees

        if (windMph == null || windKph == null || windDirection == null || windMph < 0 || windKph < 0 || windDirection < 0) {
            return buildUpdate(dataType)
        }

        val unit = settingsManager.getSpeedUnit()
        val speedVal: Int
        val speedUnit: String
        val speedUnitShort: String

        when (unit) {
            Units.MILES_PER_HOUR -> {
                speedVal = windMph.roundToInt()
                speedUnit = getString(sharedRes.string.unit_mph).also { speedUnitShort = it }
            }
            Units.KILOMETERS_PER_HOUR -> {
                speedVal = windKph.roundToInt()
                speedUnit = getString(sharedRes.string.unit_kph).also { speedUnitShort = it }
            }
            Units.METERS_PER_SECOND -> {
                speedVal =
                    ConversionMethods.kphToMsec(windKph).roundToInt()
                speedUnit = getString(sharedRes.string.unit_msec).also { speedUnitShort = it }
            }
            Units.KNOTS -> {
                speedVal =
                    ConversionMethods.mphToKts(windMph).roundToInt()
                speedUnit = getString(sharedRes.string.unit_knots)
                speedUnitShort = "kn"
            }
            else -> {
                speedVal = windMph.roundToInt()
                speedUnit = getString(sharedRes.string.unit_mph).also { speedUnitShort = it }
            }
        }

        val windSpeedShort =
            String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnitShort)
        val windSpeedLong = String.format(
            LocaleUtils.getLocale(),
            "%d %s, %s",
            speedVal,
            speedUnit,
            getWindDirection(windDirection.toFloat())
        )

        return buildUpdate(dataType, windSpeedShort, windSpeedLong, windDirection)
    }

    private fun buildUpdate(
        dataType: ComplicationType,
        windSpeedShort: String? = null, windSpeedLong: String? = null, windDirection: Int = 0
    ): ComplicationData? {
        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val icon = Icon.createWithBitmap(
            ImageUtils.rotateBitmap(
                ImageUtils.bitmapFromDrawable(
                    getThemeContextOverride(false),
                    wim.getWeatherIconResource(complicationIcon)
                ), windDirection.toFloat() + 180
            )
        )
        val monochromaticIcon = Icon.createWithBitmap(
            ImageUtils.rotateBitmap(
                ImageUtils.bitmapFromDrawable(
                    getThemeContextOverride(false),
                    wip.getWeatherIconResource(complicationIcon)
                ), windDirection.toFloat() + 180
            )
        ).setTint(Colors.WHITESMOKE)

        return when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(windSpeedShort ?: WeatherIcons.EM_DASH)
                        .build(),
                    PlainComplicationText.Builder(
                        windSpeedLong
                            ?: "${getString(sharedRes.string.label_wind)}: ${getString(sharedRes.string.weather_notavailable)}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(sharedRes.string.label_wind)).build(),
                    PlainComplicationText.Builder(
                        windSpeedLong
                            ?: "${getString(sharedRes.string.label_wind)}: ${getString(sharedRes.string.weather_notavailable)}"
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(windSpeedLong ?: WeatherIcons.EM_DASH).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(icon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    PlainComplicationText.Builder("${getString(sharedRes.string.label_wind)}: $windSpeedLong")
                        .build()
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(icon)
                        .build(),
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_wind)}: ${
                            windSpeedLong ?: getString(
                                sharedRes.string.weather_notavailable
                            )
                        }"
                    )
                        .build()
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }

            else -> {
                null
            }
        }
    }
}