package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.controls.BeaufortViewModel
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
        setOf(ComplicationType.RANGED_VALUE)
    private val complicationIconResId = R.drawable.wi_strong_wind

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 12f,
                    PlainComplicationText.Builder(getString(R.string.label_beaufort)).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("3").build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_beaufort)).build()
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
                    PlainComplicationText.Builder(beaufortModel.beaufort.value).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(beaufortModel.progress.toString()).build()
                ).setTitle(
                    PlainComplicationText.Builder(beaufortModel.beaufort.label).build()
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