package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate

class AQIComplicationService : BaseWeatherComplicationService() {
    companion object {
        private const val TAG = "AQIComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT
        )
    private val complicationIconResId = R.drawable.wi_cloud

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (!supportedComplicationTypes.contains(request.complicationType)) {
            Timber.tag(TAG).d("Complication %d no update required", request.complicationInstanceId)
            return NoDataComplicationData()
        }

        return scope.async {
            var complicationData: ComplicationData? = null

            if (settingsManager.isWeatherLoaded()) {
                complicationData = settingsManager.getHomeData()?.let { locData ->
                    val weather = withContext(Dispatchers.IO) {
                        try {
                            WeatherDataLoader(locData)
                                .loadWeatherData(
                                    WeatherRequest.Builder()
                                        .loadForecasts()
                                        .forceLoadSavedData()
                                        .build()
                                )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val today = LocalDate.now(locData.tzOffset)

                    val aqiIndex =
                        weather?.condition?.airQuality?.index
                            ?: settingsManager.getWeatherForecastData(
                                locData.query
                            )?.aqiForecast?.firstOrNull {
                                today.isEqual(it.date)
                            }?.index ?: return@let null

                    buildUpdate(request.complicationType, aqiIndex)
                }
            }

            if (complicationData != null) {
                Timber.tag(TAG).d("Complication %d updated", request.complicationInstanceId)
                return@async complicationData
            } else {
                // If no data is sent, we still need to inform the ComplicationManager, so
                // the update job can finish and the wake lock isn't held any longer.
                Timber.tag(TAG)
                    .d("Complication %d no update required", request.complicationInstanceId)
                return@async NoDataComplicationData()
            }
        }.await()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    57f, 0f, 301f,
                    PlainComplicationText.Builder("Air Quality: 57, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("57").build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
                )*/.build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("57").build(),
                    PlainComplicationText.Builder("Air Quality: 57, Moderate").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
                )*/.build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Air Quality").build(),
                    PlainComplicationText.Builder("Air Quality: 57, Moderate").build()
                )/*.setTitle(
                    PlainComplicationText.Builder("57, Moderate").build()
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

    private fun buildUpdate(dataType: ComplicationType, aqiIndex: Int): ComplicationData? {
        if (!supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val aqiModel = AirQualityViewModel(AirQuality().apply {
            index = aqiIndex
        })

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    aqiModel.progress.toFloat(), 0f, aqiModel.progressMax.toFloat(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_airquality_short)}: $aqiIndex, ${aqiModel.level}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(aqiIndex.toString()).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
                )*/.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(aqiIndex.toString()).build(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_airquality_short)}: $aqiIndex, ${aqiModel.level}"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
                )*/
                .setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build(),
                    PlainComplicationText.Builder(
                        "${getString(R.string.label_airquality_short)}: $aqiIndex, ${aqiModel.level}"
                    ).build()
                )/*.setTitle(
                    PlainComplicationText.Builder("$aqiIndex, ${aqiModel.level}").build()
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
