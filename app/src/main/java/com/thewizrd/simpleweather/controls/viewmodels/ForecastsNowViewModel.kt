package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ForecastsNowViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var forecastData = MutableLiveData<List<Forecast>>()
    private var hourlyForecastsData = MutableLiveData<List<HourlyForecast>>()
    private var minutelyForecastData = MutableLiveData<List<MinutelyForecast>>()

    private var hourlyForecastsListData = MutableLiveData<List<HourlyForecastNowViewModel>>()

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    fun getForecastData(): LiveData<List<Forecast>> {
        return forecastData
    }

    fun getHourlyForecastData(): LiveData<List<HourlyForecast>> {
        return hourlyForecastsData
    }

    fun getMinutelyForecastData(): LiveData<List<MinutelyForecast>> {
        return minutelyForecastData
    }

    fun getHourlyForecastListData(): LiveData<List<HourlyForecastNowViewModel>> {
        return hourlyForecastsListData
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

                currentForecastsData?.removeObserver(forecastObserver)
                currentForecastsData = withContext(Dispatchers.IO) {
                    weatherDAO.getLiveForecastData(location.query)
                }
                currentForecastsData!!.observeForever(forecastObserver)

                forecastData.postValue(currentForecastsData?.value?.forecast)
                minutelyForecastData.postValue(currentForecastsData?.value?.minForecast)

                currentHrForecastsData?.removeObserver(hrforecastObserver)
                currentHrForecastsData = withContext(Dispatchers.IO) {
                    val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query,
                        12,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    )
                }
                currentHrForecastsData!!.observeForever(hrforecastObserver)

                hourlyForecastsData.postValue(currentHrForecastsData?.value)
                hourlyForecastsListData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            if (currentHrForecastsData?.value != null) {
                hourlyForecastsListData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
            }
        }
    }

    private val hrForecastMapper = Function<List<HourlyForecast>?, List<HourlyForecastNowViewModel>> { input ->
        input?.map { HourlyForecastNowViewModel(it) } ?: emptyList()
    }

    private val precipMinGraphMapper = Function<Forecasts?, List<MinutelyForecast>?> { input ->
        val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
        val now = ZonedDateTime.now(
            locationData?.tzOffset
                ?: ZoneOffset.UTC
        ).minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS)
        input?.minForecast?.filter { !it.date.isBefore(now) }?.takeUnless { it.isEmpty() }?.take(60)
    }

    private val forecastObserver: Observer<Forecasts> = Observer<Forecasts> { forecastData ->
        this.forecastData.postValue(forecastData?.forecast)
        this.minutelyForecastData.postValue(precipMinGraphMapper.apply(forecastData))
    }

    private val hrforecastObserver: Observer<List<HourlyForecast>> = Observer<List<HourlyForecast>> { forecastData ->
        this.hourlyForecastsData.postValue(forecastData)
        this.hourlyForecastsListData.postValue(hrForecastMapper.apply(forecastData))
    }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentForecastsData?.removeObserver(forecastObserver)
        currentHrForecastsData?.removeObserver(hrforecastObserver)

        currentForecastsData = null
        currentHrForecastsData = null
    }
}