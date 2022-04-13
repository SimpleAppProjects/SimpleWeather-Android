package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class WeatherHiLoComplicationService : WeatherForecastComplicationService() {
    companion object {
        private const val TAG = "WeatherHiLoComplicationService"
    }

    override val supportedComplicationTypes =
        setOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)
    private val complicationIconResId = R.drawable.wi_day_sunny

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("70°").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setTitle(
                    PlainComplicationText.Builder("75° | 65°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("70° - Sunny").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setTitle(
                    PlainComplicationText.Builder("75° | 65°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
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
        if (weather == null || !weather.isValid || dataType != ComplicationType.SHORT_TEXT && dataType != ComplicationType.LONG_TEXT) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == settingsMgr.getTemperatureUnit()

        var shouldHideHi = false
        var shouldHideLo = false

        // Temperature
        val currTemp =
            if (weather.condition.tempF != null && weather.condition.tempF != weather.condition.tempC) {
                val temp =
                    if (isFahrenheit) Math.round(weather.condition.tempF) else Math.round(weather.condition.tempC)
                String.format(LocaleUtils.getLocale(), "%d", temp)
            } else {
                WeatherIcons.PLACEHOLDER
            }

        val hiTemp =
            if (weather.condition.highF != null && weather.condition.highF != weather.condition.highC) {
                val temp =
                    if (isFahrenheit) Math.round(weather.condition.highF) else Math.round(weather.condition.highC)
                String.format(LocaleUtils.getLocale(), "%d°", temp)
            } else if (forecast?.highF != null && forecast.highF != forecast.highC) {
                val temp =
                    if (isFahrenheit) Math.round(forecast.highF) else Math.round(forecast.highC)
                String.format(LocaleUtils.getLocale(), "%d°", temp)
            } else {
                shouldHideHi = true
                WeatherIcons.PLACEHOLDER
            }

        val loTemp =
            if (weather.condition.lowF != null && weather.condition.lowF != weather.condition.lowC) {
                val temp =
                    if (isFahrenheit) Math.round(weather.condition.lowF) else Math.round(weather.condition.lowC)
                String.format(LocaleUtils.getLocale(), "%d°", temp)
            } else if (forecast?.lowF != null && forecast.lowF != forecast.lowC) {
                val temp =
                    if (isFahrenheit) Math.round(forecast.lowF) else Math.round(forecast.lowC)
                String.format(LocaleUtils.getLocale(), "%d°", temp)
            } else {
                shouldHideLo = true
                WeatherIcons.PLACEHOLDER
            }

        val showHiLo = (!shouldHideHi || !shouldHideLo) && hiTemp != loTemp

        val tempUnit = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS

        val temp = String.format(LocaleUtils.getLocale(), "%s°%s", currTemp, tempUnit)

        // Condition text
        val provider = WeatherManager.getProvider(weather.source)
        val condition = if (provider.supportsWeatherLocale()) {
            weather.condition.weather
        } else {
            provider.getWeatherCondition(weather.condition.icon)
        }

        val wim = WeatherIconsManager.getInstance()
        val weatherIcon = wim.getWeatherIconResource(weather.condition.icon)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                weatherIcon
            )
        )

        when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(
                        if (showHiLo) {
                            "$hiTemp/$loTemp"
                        } else {
                            "$currTemp°"
                        }
                    ).build(),
                    PlainComplicationText.Builder("$temp - $condition; Hi: $hiTemp, Lo: $loTemp")
                        .build()
                )

                builder.setMonochromaticImage(
                    MonochromaticImage.Builder(icon).apply {
                        // Weather Icon
                        if (!wim.isFontIcon) {
                            val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                            setAmbientImage(
                                Icon.createWithBitmap(
                                    ImageUtils.tintedBitmapFromDrawable(
                                        this@WeatherHiLoComplicationService,
                                        wip.getWeatherIconResource(weather.condition.icon),
                                        Colors.WHITE
                                    )
                                )
                            )
                        }
                    }
                        .build()
                )

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }
            ComplicationType.LONG_TEXT -> {
                val builder = LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("$temp - $condition").build(),
                    PlainComplicationText.Builder("$temp - $condition").build()
                ).setTitle(
                    PlainComplicationText.Builder("$hiTemp | $loTemp").build()
                )

                // Weather Icon
                if (wim.isFontIcon) {
                    builder.setMonochromaticImage(
                        MonochromaticImage.Builder(icon).build()
                    )
                } else {
                    val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                    builder.setSmallImage(
                        SmallImage.Builder(icon, SmallImageType.ICON)
                            .setAmbientImage(
                                Icon.createWithBitmap(
                                    ImageUtils.tintedBitmapFromDrawable(
                                        this,
                                        wip.getWeatherIconResource(weather.condition.icon),
                                        Colors.WHITE
                                    )
                                )
                            )
                            .build()
                    )
                }

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }
            else -> {
                return null
            }
        }
    }
}