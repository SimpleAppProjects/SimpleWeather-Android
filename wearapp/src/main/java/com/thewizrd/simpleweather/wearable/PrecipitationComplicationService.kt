package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class PrecipitationComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "PrecipitationComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(ComplicationType.RANGED_VALUE)

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    50f, 0f, 100f,
                    PlainComplicationText.Builder(getString(R.string.label_chance)).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            R.drawable.wi_umbrella
                        )
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("50%").build()
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

        val popChance = weather.precipitation?.pop ?: hourlyForecast?.extras?.pop ?: return null

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    popChance.toFloat(), 0f, 100f,
                    PlainComplicationText.Builder(getString(R.string.label_chance)).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            R.drawable.wi_umbrella
                        )
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("$popChance%").build()
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