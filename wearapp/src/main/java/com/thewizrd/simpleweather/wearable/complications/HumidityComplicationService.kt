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
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

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
    private val complicationIconResId = sharedRes.drawable.wi_humidity

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    75f, 0f, 100f,
                    PlainComplicationText.Builder("${getString(sharedRes.string.label_humidity)}: 75%")
                        .build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
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
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).build()
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
        hourlyForecast: HourlyForecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val humidityPct = weather.atmosphere?.humidity ?: hourlyForecast?.extras?.humidity
        val humidityStr = humidityPct?.let { "${it}%" } ?: WeatherIcons.EM_DASH

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    humidityPct?.toFloat() ?: 0f, 0f, 100f,
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_humidity)}: $humidityStr"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
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
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setTapAction(
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
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
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