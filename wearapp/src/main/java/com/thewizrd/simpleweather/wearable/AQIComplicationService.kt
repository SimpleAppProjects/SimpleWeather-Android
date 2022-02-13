package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
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

    override val supportedComplicationTypes = setOf(ComplicationType.RANGED_VALUE)
    private val complicationIconResId = R.drawable.wi_cloud

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (!supportedComplicationTypes.contains(request.complicationType)) {
            Timber.tag(TAG).d("Complication %d no update required", request.complicationInstanceId)
            return NoDataComplicationData()
        }

        return scope.async {
            var complicationData: ComplicationData? = null

            if (settingsMgr.isWeatherLoaded()) {
                complicationData = settingsMgr.getHomeData()?.let { locData ->
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
                        weather?.condition?.airQuality?.index ?: settingsMgr.getWeatherForecastData(
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
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).setText(
                    PlainComplicationText.Builder("57").build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
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
                    PlainComplicationText.Builder(aqiModel.description).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(aqiIndex.toString()).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_airquality_short))
                        .build()
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