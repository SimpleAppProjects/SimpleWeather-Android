package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ForecastsNowViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var forecastData = MutableStateFlow<List<Forecast>?>(null)
    private var hourlyForecastsData = MutableStateFlow<List<HourlyForecast>?>(null)
    private var minutelyForecastData = MutableStateFlow<List<MinutelyForecast>?>(null)

    private var hourlyForecastsListData = MutableStateFlow<List<HourlyForecastNowViewModel>?>(null)

    private var currentForecastsData: Flow<Forecasts> = emptyFlow()
    private var currentHrForecastsData: Flow<List<HourlyForecast>> = emptyFlow()

    private var flowScope: CoroutineScope? = null

    fun getForecastData(): StateFlow<List<Forecast>?> {
        return forecastData
    }

    fun getHourlyForecastData(): StateFlow<List<HourlyForecast>?> {
        return hourlyForecastsData
    }

    fun getMinutelyForecastData(): StateFlow<List<MinutelyForecast>?> {
        return minutelyForecastData
    }

    fun getHourlyForecastListData(): StateFlow<List<HourlyForecastNowViewModel>?> {
        return hourlyForecastsListData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                flowScope?.cancel()
                flowScope = CoroutineScope(SupervisorJob())

                unitCode = settingsManager.getUnitString()
                localeCode = LocaleUtils.getLocaleCode()
                iconProvider = settingsManager.getIconsProvider()

                currentForecastsData =
                    weatherDAO.getLiveForecastData(location.query).filterNotNull()
                        .distinctUntilChanged()

                flowScope?.launch {
                    currentForecastsData.collect {
                        forecastData.emit(it.forecast)
                        minutelyForecastData.emit(minForecastMapper.apply(it))
                    }
                }

                val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
                currentHrForecastsData =
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query,
                        24,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    ).distinctUntilChanged()

                flowScope?.launch {
                    currentHrForecastsData.collect {
                        hourlyForecastsData.emit(it)
                        hourlyForecastsListData.emit(hrForecastMapper.apply(it))
                    }
                }
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            flowScope?.launch {
                hourlyForecastsListData.emit(hrForecastMapper.apply(currentHrForecastsData.last()))
            }
        }
    }

    private val hrForecastMapper = Function<List<HourlyForecast>?, List<HourlyForecastNowViewModel>> { input ->
        input?.map { HourlyForecastNowViewModel(it) } ?: emptyList()
    }

    private val minForecastMapper = Function<Forecasts?, List<MinutelyForecast>?> { input ->
        val now = ZonedDateTime.now(
            locationData?.tzOffset
                ?: ZoneOffset.UTC
        ).truncatedTo(ChronoUnit.HOURS)
        input?.minForecast?.filter { !it.date.isBefore(now) }
    }

    override fun onCleared() {
        super.onCleared()

        flowScope?.cancel()
        locationData = null
    }
}