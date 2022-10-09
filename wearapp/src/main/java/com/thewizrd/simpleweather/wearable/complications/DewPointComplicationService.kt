package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import kotlin.math.roundToInt

class DewPointComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "DewPointComplicationService"
    }

    override val supportedComplicationTypes =
        setOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)
    private val complicationIconResId = R.drawable.wi_thermometer

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("38°").build(),
                    PlainComplicationText.Builder("Dew Point: 38°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_dewpoint)).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_dewpoint)).build(),
                    PlainComplicationText.Builder("Dew Point: 38°").build()
                ).setTitle(
                    PlainComplicationText.Builder("38°").build()
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

        val dewPointF =
            weather.atmosphere?.dewpointF ?: hourlyForecast?.extras?.dewpointF ?: return null
        val dewPointC =
            weather.atmosphere?.dewpointC ?: hourlyForecast?.extras?.dewpointC ?: return null

        if (dewPointF == dewPointC) return null

        val tempUnit = settingsManager.getTemperatureUnit()
        val tempVal =
            if (tempUnit == Units.FAHRENHEIT) dewPointF.roundToInt() else dewPointC.toInt()
        val tempStr = String.format("$tempVal°$tempUnit")

        return when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(tempStr).build(),
                    PlainComplicationText.Builder(
                        String.format("%s: %s", getString(R.string.label_dewpoint), tempStr)
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_dewpoint)).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_dewpoint)).build(),
                    PlainComplicationText.Builder(
                        String.format("%s: %s", getString(R.string.label_dewpoint), tempStr)
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(tempStr).build()
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