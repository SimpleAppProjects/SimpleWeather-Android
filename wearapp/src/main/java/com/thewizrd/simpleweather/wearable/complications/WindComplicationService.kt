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
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getWindDirection
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import kotlin.math.roundToInt

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
    private val complicationIconResId = R.drawable.wi_strong_wind

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("5 mph").build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), 330.0f // 150° + 180
                            )
                        )
                    ).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Wind").build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
                ).setTitle(
                    PlainComplicationText.Builder("5 mph, SSE").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), 330.0f // 150° + 180
                            )
                        )
                    ).build(),
                    PlainComplicationText.Builder("Wind: 5 mph, SSE").build()
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), 330.0f // 150° + 180
                            )
                        ),
                        SmallImageType.ICON
                    ).setAmbientImage(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), 330.0f // 150° + 180
                            )
                        )
                    ).build(),
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

        val windMph = weather.condition?.windMph ?: hourlyForecast?.windMph ?: return null
        val windKph = weather.condition?.windKph ?: hourlyForecast?.windKph ?: return null
        val windDirection =
            weather.condition?.windDegrees ?: hourlyForecast?.windDegrees ?: return null

        if (windMph < 0 || windKph < 0 || windDirection < 0) return null

        val unit = settingsManager.getSpeedUnit()
        val speedVal: Int
        val speedUnit: String

        when (unit) {
            Units.MILES_PER_HOUR -> {
                speedVal = windMph.roundToInt()
                speedUnit = getString(R.string.unit_mph)
            }
            Units.KILOMETERS_PER_HOUR -> {
                speedVal = windKph.roundToInt()
                speedUnit = getString(R.string.unit_kph)
            }
            Units.METERS_PER_SECOND -> {
                speedVal =
                    ConversionMethods.kphToMsec(windKph).roundToInt()
                speedUnit = getString(R.string.unit_msec)
            }
            else -> {
                speedVal = windMph.roundToInt()
                speedUnit = getString(R.string.unit_mph)
            }
        }

        val windSpeedShort = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
        val windSpeedLong = String.format(
            LocaleUtils.getLocale(),
            "%d %s, %s",
            speedVal,
            speedUnit,
            getWindDirection(windDirection.toFloat())
        )

        return when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(windSpeedShort).build(),
                    PlainComplicationText.Builder(windSpeedLong).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), windDirection.toFloat() + 180
                            )
                        )
                    ).build()
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_wind)).build(),
                    PlainComplicationText.Builder(windSpeedLong).build()
                ).setTitle(
                    PlainComplicationText.Builder(windSpeedLong).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), windDirection.toFloat() + 180
                            )
                        )
                            .setTint(Colors.WHITESMOKE)
                    ).build(),
                    PlainComplicationText.Builder("${getString(R.string.label_wind)}: $windSpeedLong")
                        .build()
                ).build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), windDirection.toFloat() + 180
                            )
                        ),
                        SmallImageType.ICON
                    ).setAmbientImage(
                        Icon.createWithBitmap(
                            ImageUtils.rotateBitmap(
                                ImageUtils.bitmapFromDrawable(
                                    getThemeContextOverride(false),
                                    R.drawable.wi_wind_direction_white
                                ), windDirection.toFloat() + 180
                            )
                        )
                            .setTint(Colors.WHITESMOKE)
                    ).build(),
                    PlainComplicationText.Builder("${getString(R.string.label_wind)}: $windSpeedLong")
                        .build()
                ).build()
            }

            else -> {
                null
            }
        }
    }
}