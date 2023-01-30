package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.common.controls.BeaufortViewModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class BeaufortComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "BeaufortComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )
    private val complicationIconResId = R.drawable.wi_strong_wind

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 12f,
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("3").build()
                )/*.setTitle(
                    PlainComplicationText.Builder("Beaufort").build()
                )*/.build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("3").build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder("Beaufort").build()
                )*/.build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Beaufort").build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                )/*.setTitle(
                    PlainComplicationText.Builder("3, Gentle Breeze").build()
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

        val beaufort = weather.condition.beaufort ?: hourlyForecast?.extras?.windMph?.let {
            Beaufort(getBeaufortScale(it))
        } ?: return null
        val beaufortModel = BeaufortViewModel(beaufort)

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    beaufortModel.progress.toFloat(), 0f, beaufortModel.progressMax.toFloat(),
                    PlainComplicationText.Builder(
                        "${beaufortModel.beaufort.label}: ${beaufortModel.progress}, ${beaufortModel.beaufort.value}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(beaufortModel.progress.toString()).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(beaufortModel.beaufort.label).build()
                )*/.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(beaufortModel.progress.toString()).build(),
                    PlainComplicationText.Builder(
                        "${beaufortModel.beaufort.label}: ${beaufortModel.progress}, ${beaufortModel.beaufort.value}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(beaufortModel.beaufort.label).build()
                )*/.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(beaufortModel.beaufort.label).build(),
                    PlainComplicationText.Builder(
                        "${beaufortModel.beaufort.label}: ${beaufortModel.progress}, ${beaufortModel.beaufort.value}"
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(
                        "${beaufortModel.progress}, ${beaufortModel.beaufort.value}"
                    ).build()
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
