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
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.AirQualityUtils.getIndexFromData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import kotlin.math.max
import com.thewizrd.shared_resources.R as sharedRes

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
    private val complicationIconResId = sharedRes.drawable.wi_cloud

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
                        val result = try {
                            WeatherDataLoader(locData)
                                .loadWeatherResult(
                                    WeatherRequest.Builder()
                                        .loadForecasts()
                                        .forceLoadSavedData()
                                        .build()
                                )
                        } catch (e: Exception) {
                            null
                        }

                        result?.data
                    }

                    val today = LocalDate.now(locData.tzOffset)

                    val aqi =
                        weather?.condition?.airQuality ?: settingsManager.getWeatherForecastData(
                            locData.query
                        )?.aqiForecast?.firstOrNull { today.isEqual(it.date) }

                    buildUpdate(request.complicationType, aqi)
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
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).build()
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
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Air Quality").build(),
                    PlainComplicationText.Builder("Air Quality: 57, Moderate").build()
                ).setTitle(
                    PlainComplicationText.Builder("57, Moderate").build()
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

    private fun buildUpdate(dataType: ComplicationType, aqi: AirQuality?): ComplicationData? {
        if (!supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val aqiModel = aqi?.apply { if (index == null) index = getIndexFromData() }
            ?.takeIf { it.index != null }?.let { AirQualityViewModel(it) }
        val aqiStr = aqiModel?.let { "${it.index}, ${it.level}" } ?: WeatherIcons.EM_DASH
        val aqiShortStr = aqiModel?.let { "${it.index}" } ?: WeatherIcons.EM_DASH
        val aqiProgress = aqiModel?.progress?.toFloat() ?: 0f
        val aqiProgressMax = aqiModel?.let { max(it.progressMax, it.progress).toFloat() } ?: 301f

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    aqiProgress, 0f, aqiProgressMax,
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_airquality_short)}: $aqiStr"
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, complicationIconResId)
                            .setTint(Colors.WHITESMOKE)
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(aqiShortStr).build()
                ).setTapAction(
                    getTapIntent(this)
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(aqiShortStr).build(),
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_airquality_short)}: $aqiStr"
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
                    PlainComplicationText.Builder(getString(sharedRes.string.label_airquality_short))
                        .build(),
                    PlainComplicationText.Builder(
                        "${getString(sharedRes.string.label_airquality_short)}: $aqiStr"
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(aqiStr).build()
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