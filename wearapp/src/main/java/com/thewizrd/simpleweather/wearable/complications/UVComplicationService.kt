package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.common.controls.UVIndexViewModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.UV
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class UVComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "UVComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )
    private val complicationIconResId = R.drawable.wi_day_sunny

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 11f,
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("3").build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
                )*/.build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("3").build(),
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
                )*/.build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build(),
                    PlainComplicationText.Builder("UV Index: 3, Moderate").build()
                )/*.setTitle(
                    PlainComplicationText.Builder("3, Moderate").build()
                )*/.setMonochromaticImage(
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

        val uvIndex = weather.condition.uv?.index ?: hourlyForecast?.extras?.uvIndex ?: return null
        val uvModel = UVIndexViewModel(UV(uvIndex))

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    uvModel.progress.toFloat(), 0f, uvModel.progressMax.toFloat(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_uv)}: ${uvModel.index}, ${uvModel.description}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(uvModel.index.toString()).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
                )*/.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(uvModel.index.toString()).build(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_uv)}: ${uvModel.index}, ${uvModel.description}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
                )*/.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_uv)}: ${uvModel.index}, ${uvModel.description}"
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder("${uvModel.index}, ${uvModel.description}")
                        .build()
                )*/.setMonochromaticImage(
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
