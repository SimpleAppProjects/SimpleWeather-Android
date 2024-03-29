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
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.weather_api.weatherModule

class WeatherHiLoComplicationService : WeatherForecastComplicationService() {
    companion object {
        private const val TAG = "WeatherHiLoComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
    private val complicationIconResId = R.drawable.wi_day_sunny

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("75°/65°").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
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
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()

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
        val provider = weatherModule.weatherManager.getWeatherProvider(weather.source)
        val condition = if (provider.supportsWeatherLocale()) {
            weather.condition.weather
        } else {
            provider.getWeatherCondition(weather.condition.icon)
        }

        val wim = sharedDeps.weatherIconsManager
        val weatherIcon = wim.getWeatherIconResource(weather.condition.icon)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                weatherIcon
            )
        )

        val contentDescription = PlainComplicationText.Builder(
            "$temp - $condition; ${getString(R.string.label_high)}: $hiTemp, ${getString(R.string.label_low)}: $loTemp"
        ).build()

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
                    contentDescription
                )

                builder.setMonochromaticImage(
                    MonochromaticImage.Builder(icon).apply {
                        // Weather Icon
                        if (!wim.isFontIcon) {
                            val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)
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
                    contentDescription
                ).setTitle(
                    PlainComplicationText.Builder("$hiTemp | $loTemp").build()
                )

                // Weather Icon
                if (wim.isFontIcon) {
                    builder.setMonochromaticImage(
                        MonochromaticImage.Builder(icon).build()
                    )
                } else {
                    val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)
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
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

                return MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            wip.getWeatherIconResource(weather.condition.icon)
                        )
                            .setTint(Colors.WHITESMOKE)
                    ).build(),
                    contentDescription
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SMALL_IMAGE -> {
                val wip = wim.getIconProvider(WeatherIconsEFProvider.KEY)

                return SmallImageComplicationData.Builder(
                    SmallImage.Builder(
                        icon,
                        SmallImageType.ICON
                    ).setAmbientImage(
                        Icon.createWithBitmap(
                            ImageUtils.tintedBitmapFromDrawable(
                                this,
                                wip.getWeatherIconResource(weather.condition.icon),
                                Colors.WHITE
                            )
                        )
                    ).build(),
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