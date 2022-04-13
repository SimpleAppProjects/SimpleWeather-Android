package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.controls.UVIndexViewModel
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
        setOf(ComplicationType.RANGED_VALUE)
    private val complicationIconResId = R.drawable.wi_day_sunny

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 11f,
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("3").build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
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
                    PlainComplicationText.Builder(uvModel.description).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(uvModel.index.toString()).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_uv)).build()
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