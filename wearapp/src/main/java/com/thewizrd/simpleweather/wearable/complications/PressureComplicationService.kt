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
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min
import com.thewizrd.shared_resources.R as sharedRes

class PressureComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "PressureComplicationService"
    }

    override val supportedComplicationTypes =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )

    private val complicationIcon = WeatherIcons.BAROMETER

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
                    30.3f, 26f, 32f,
                    PlainComplicationText.Builder("Pressure: 30.3 inHg").build()
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
                }.setText(
                    PlainComplicationText.Builder("30.3 in").build()
                ).build()
            }

            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("30.3 in").build(),
                    PlainComplicationText.Builder("Pressure: 30.3 inHg").build()
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_pressure))
                        .build(),
                    PlainComplicationText.Builder("Pressure: 30.3 inHg").build()
                ).setTitle(
                    PlainComplicationText.Builder("30.3 inHg").build()
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

        val pressureIn =
            weather.atmosphere?.pressureIn ?: hourlyForecast?.extras?.pressureIn
        val pressureMb =
            weather.atmosphere?.pressureMb ?: hourlyForecast?.extras?.pressureMb

        if (pressureIn == null || pressureMb == null || pressureIn < 0 || pressureMb < 0) {
            return buildUpdate(dataType)
        }

        val df = DecimalFormat.getInstance(LocaleUtils.getLocale()) as DecimalFormat
        df.applyPattern("0.#")

        val unit = settingsManager.getPressureUnit()
        val pressureVal: String
        val pressureUnit: String
        val pressureUnitShort: String

        when (unit) {
            Units.INHG -> {
                pressureVal = df.format(pressureIn)
                pressureUnit = getString(sharedRes.string.unit_inHg)
                pressureUnitShort = getString(sharedRes.string.unit_in)
            }

            Units.MILLIBAR -> {
                pressureVal = df.format(pressureMb)
                pressureUnit = getString(sharedRes.string.unit_mBar).also { pressureUnitShort = it }
            }

            Units.MMHG -> {
                pressureVal = df.format(ConversionMethods.inHgToMmHg(pressureIn))
                pressureUnit = getString(sharedRes.string.unit_mmHg)
                pressureUnitShort = getString(sharedRes.string.unit_mm)
            }

            else -> {
                pressureVal = df.format(pressureIn)
                pressureUnit = getString(sharedRes.string.unit_inHg)
                pressureUnitShort = getString(sharedRes.string.unit_in)
            }
        }

        val pressureStr = String.format(LocaleUtils.getLocale(), "%s %s", pressureVal, pressureUnit)
        val pressureStrShort =
            String.format(LocaleUtils.getLocale(), "%s %s", pressureVal, pressureUnitShort)

        return buildUpdate(dataType, pressureStr, pressureStrShort, pressureIn)
    }

    private fun buildUpdate(
        dataType: ComplicationType,
        pressureStr: String? = null, pressureStrShort: String? = null, pressureInVal: Float? = null
    ): ComplicationData? {
        val wim = sharedDeps.weatherIconsManager
        val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

        val pressureProgress = pressureInVal ?: 26f
        val pressureMin = min(pressureProgress, 26f)
        val pressureMax = max(pressureProgress, 32f)

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
                    pressureProgress, pressureMin, pressureMax,
                    PlainComplicationText.Builder(
                        String.format(
                            "%s: %s",
                            getString(sharedRes.string.label_pressure),
                            pressureStr ?: getString(sharedRes.string.weather_notavailable)
                        )
                    ).build()
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
                }.setText(
                    PlainComplicationText.Builder(pressureStrShort ?: WeatherIcons.EM_DASH)
                        .build()
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }

            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(pressureStrShort ?: WeatherIcons.EM_DASH)
                        .build(),
                    PlainComplicationText.Builder(
                        String.format(
                            "%s: %s",
                            getString(sharedRes.string.label_pressure),
                            pressureStr ?: getString(sharedRes.string.weather_notavailable)
                        )
                    ).build()
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_pressure))
                        .build(),
                    PlainComplicationText.Builder(
                        String.format(
                            "%s: %s",
                            getString(sharedRes.string.label_pressure),
                            pressureStr ?: getString(sharedRes.string.weather_notavailable)
                        )
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(pressureStr ?: WeatherIcons.EM_DASH).build()
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