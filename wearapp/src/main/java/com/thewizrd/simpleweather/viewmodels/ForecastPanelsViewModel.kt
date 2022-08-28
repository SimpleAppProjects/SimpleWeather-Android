package com.thewizrd.simpleweather.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

class ForecastPanelsViewModel(app: Application) : AndroidViewModel(app) {
    private var locationData: LocationData? = null
    private var unitCode: String? = null
    private var localeCode: String? = null
    private var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private val forecasts = MutableStateFlow<List<ForecastItemViewModel>>(emptyList())
    private val hourlyForecasts = MutableStateFlow<List<HourlyForecastItemViewModel>>(emptyList())
    private val minutelyForecasts = MutableStateFlow<List<MinutelyForecastViewModel>>(emptyList())

    private var currentForecastsData: Flow<Forecasts> = emptyFlow()
    private var currentHrForecastsData: Flow<List<HourlyForecast>> = emptyFlow()

    private var flowScope: CoroutineScope? = null

    fun getForecasts(): StateFlow<List<ForecastItemViewModel>> {
        return forecasts
    }

    fun getHourlyForecasts(): StateFlow<List<HourlyForecastItemViewModel>> {
        return hourlyForecasts
    }

    fun getMinutelyForecasts(): StateFlow<List<MinutelyForecastViewModel>> {
        return minutelyForecasts
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                unitCode = settingsManager.getUnitString()
                localeCode = LocaleUtils.getLocaleCode()
                iconProvider = settingsManager.getIconsProvider()

                flowScope?.cancel()

                currentForecastsData = weatherDAO.getLiveForecastData(location.query).asFlow()

                val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
                currentHrForecastsData =
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query, 6,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    ).asFlow()

                flowScope = CoroutineScope(SupervisorJob())
                flowScope?.launch {
                    currentForecastsData.collect {
                        forecasts.emit(forecastMapper.apply(it))
                    }
                }
                flowScope?.launch {
                    currentHrForecastsData.collect {
                        hourlyForecasts.emit(hrForecastMapper.apply(it))
                    }
                }
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
            !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
            !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())
        ) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            viewModelScope.launch {
                currentForecastsData.lastOrNull()?.let {
                    forecasts.emit(forecastMapper.apply(it))
                    minutelyForecasts.emit(minForecastMapper.apply(it))
                }
                currentHrForecastsData.lastOrNull()?.let {
                    hourlyForecasts.emit(hrForecastMapper.apply(it))
                }
            }
        }
    }

    private val forecastMapper = Function<Forecasts?, List<ForecastItemViewModel>> { input ->
        if (input?.forecast?.isNotEmpty() == true) {
            val totalCount = input.forecast.size
            val models = ArrayList<ForecastItemViewModel>(totalCount)

            for (i in 0 until min(totalCount, 4)) {
                models.add(ForecastItemViewModel(input.forecast[i]))
            }

            return@Function models
        }

        emptyList()
    }

    private val minForecastMapper = Function<Forecasts?, List<MinutelyForecastViewModel>> { input ->
        if (input?.minForecast?.isNotEmpty() == true) {
            val now = ZonedDateTime.now(
                locationData?.tzOffset
                    ?: ZoneOffset.UTC
            ).truncatedTo(ChronoUnit.HOURS)

            return@Function input.minForecast.filter { !it.date.isBefore(now) }.take(60).map {
                MinutelyForecastViewModel(it)
            }
        }

        emptyList()
    }

    private val hrForecastMapper =
        Function<List<HourlyForecast>?, List<HourlyForecastItemViewModel>> { input ->
            if (input != null) {
                val models = ArrayList<HourlyForecastItemViewModel>(input.size)

                for (fcast in input) {
                    models.add(HourlyForecastItemViewModel(fcast))
                }

                return@Function models
            }

            emptyList()
        }

    override fun onCleared() {
        super.onCleared()

        flowScope?.cancel()
        locationData = null
    }
}