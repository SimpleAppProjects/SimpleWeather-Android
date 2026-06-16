package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ChartsViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var currentForecastsData: Flow<Forecasts> = emptyFlow()
    private var currentHrForecastsData: Flow<List<HourlyForecast>> = emptyFlow()

    private val forecastData =
        MutableStateFlow<Pair<List<MinutelyForecast>?, List<HourlyForecast>?>?>(null)

    private var flowScope: CoroutineScope? = null

    fun getForecastData(): StateFlow<Pair<List<MinutelyForecast>?, List<HourlyForecast>?>?> {
        return forecastData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                flowScope?.cancel()

                currentHrForecastsData =
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query,
                        24,
                        ZonedDateTime.now(location.tzOffset).truncatedTo(ChronoUnit.HOURS)
                    ).distinctUntilChanged()
                currentForecastsData =
                    weatherDAO.getLiveForecastData(location.query).filterNotNull()
                        .distinctUntilChanged()

                flowScope = CoroutineScope(SupervisorJob())
                flowScope?.launch {
                    val combinedFlow =
                        combineTransform(currentForecastsData, currentHrForecastsData) { f, h ->
                            emit(f to h)
                        }
                    combinedFlow.collect {
                        forecastData.emit(graphDataMapper.apply(it))
                    }
                }
            }
        }
    }

    private val graphDataMapper =
        Function<Pair<Forecasts?, List<HourlyForecast>?>, Pair<List<MinutelyForecast>?, List<HourlyForecast>?>?> { input ->
            return@Function if (!input.first?.minForecast.isNullOrEmpty() || !input.second.isNullOrEmpty()) {
                val now = ZonedDateTime.now(
                    locationData?.tzOffset
                        ?: ZoneOffset.UTC
                ).truncatedTo(ChronoUnit.HOURS)
                Pair(
                    input.first?.minForecast?.filter { !it.date.isBefore(now) }?.take(60),
                    input.second
                )
            } else {
                null
            }
        }

    override fun onCleared() {
        super.onCleared()

        flowScope?.cancel()
        locationData = null
    }
}