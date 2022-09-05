package com.thewizrd.simpleweather.wearable.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class WeatherForecastComplicationService : BaseWeatherComplicationService() {
    companion object {
        private const val TAG = "WeatherForecastComplicationService"
    }

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
                                        .forceLoadSavedData()
                                        .build()
                                )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val forecast =
                        settingsManager.getWeatherForecastData(locData.query)?.forecast?.firstOrNull()

                    buildUpdate(request.complicationType, weather, forecast)
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
        forecast: Forecast?
    ): ComplicationData?
}