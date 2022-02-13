package com.thewizrd.simpleweather.wearable

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

abstract class WeatherHourlyForecastComplicationService : BaseWeatherComplicationService() {
    companion object {
        private const val TAG = "WeatherHourlyForecastComplicationService"
    }

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
                                        .forceLoadSavedData()
                                        .build()
                                )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
                    val nowHour = now.truncatedTo(ChronoUnit.HOURS)

                    val hrf = if (weather != null) {
                        val interval =
                            WeatherManager.getProvider(weather.source).getHourlyForecastInterval()
                        var hrf =
                            settingsMgr.getFirstHourlyForecastDataByDate(locData.query, nowHour)
                        if (hrf == null || Duration.between(now, hrf.date)
                                .toHours() > interval * 0.5
                        ) {
                            val prevHrf = settingsMgr.getFirstHourlyForecastDataByDate(
                                locData.query,
                                nowHour.minusHours(interval.toLong())
                            )
                            if (prevHrf != null) hrf = prevHrf
                        }

                        hrf
                    } else {
                        settingsMgr.getFirstHourlyForecastDataByDate(locData.query, nowHour)
                    }

                    buildUpdate(request.complicationType, weather, hrf)
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

    abstract override fun getPreviewData(type: ComplicationType): ComplicationData?

    protected abstract fun buildUpdate(
        dataType: ComplicationType,
        weather: Weather?,
        hourlyForecast: HourlyForecast?
    ): ComplicationData?
}