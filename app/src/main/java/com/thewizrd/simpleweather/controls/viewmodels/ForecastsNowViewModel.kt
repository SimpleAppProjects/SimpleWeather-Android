package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.common.collect.Lists
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tasks.AsyncTask
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.Forecasts
import com.thewizrd.shared_resources.weatherdata.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.App
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Callable
import kotlin.math.roundToLong

class ForecastsNowViewModel : ViewModel() {
    private val settingsManager = App.instance.settingsManager

    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private var forecastGraphData = MutableLiveData<RangeBarGraphViewModel>()
    private var hourlyForecastsData = MutableLiveData<List<HourlyForecastNowViewModel>>()
    private var precipitationGraphData = MutableLiveData<ForecastGraphViewModel>()

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    fun getForecastGraphData(): LiveData<RangeBarGraphViewModel> {
        return forecastGraphData
    }

    fun getHourlyForecastData(): LiveData<List<HourlyForecastNowViewModel>> {
        return hourlyForecastsData
    }

    fun getPrecipitationGraphData(): LiveData<ForecastGraphViewModel> {
        return precipitationGraphData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            // Clone location data
            locationData = LocationData(LocationQueryViewModel(location))

            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            currentForecastsData?.removeObserver(forecastObserver)
            currentForecastsData = AsyncTask.await(Callable<LiveData<Forecasts>?> {
                settingsManager.getWeatherDAO().getLiveForecastData(location.query)
            })

            currentForecastsData!!.observeForever(forecastObserver)
            forecastGraphData.postValue(forecastMapper.apply(currentForecastsData!!.value))

            currentHrForecastsData?.removeObserver(hrforecastObserver)
            currentHrForecastsData = AsyncTask.await(Callable<LiveData<List<HourlyForecast>>?> {
                val hrInterval = WeatherManager.getInstance().hourlyForecastInterval
                settingsManager.getWeatherDAO().getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(location.query, 12, ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).roundToLong()).truncatedTo(ChronoUnit.HOURS))
            })
            currentHrForecastsData!!.observeForever(hrforecastObserver)
            hourlyForecastsData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
            precipitationGraphData.postValue(precipGraphMapper.apply(currentHrForecastsData!!.value))
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            if (currentForecastsData?.value != null) {
                forecastGraphData.postValue(forecastMapper.apply(currentForecastsData!!.value))
            }
            if (currentHrForecastsData?.value != null) {
                hourlyForecastsData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
                precipitationGraphData.postValue(precipGraphMapper.apply(currentHrForecastsData!!.value))
            }
        }
    }

    private val forecastMapper: Function<Forecasts?, RangeBarGraphViewModel?> = Function<Forecasts?, RangeBarGraphViewModel?> { input ->
        if (input?.forecast?.size ?: 0 > 0) RangeBarGraphViewModel(input.forecast) else null
    }

    private val hrForecastMapper: Function<List<HourlyForecast>?, List<HourlyForecastNowViewModel>> = object : Function<List<HourlyForecast>?, List<HourlyForecastNowViewModel>> {
        override fun apply(input: List<HourlyForecast>?): List<HourlyForecastNowViewModel> {
            if (input != null) {
                return Lists.transform(input) { HourlyForecastNowViewModel(it!!) }
            }
            return Collections.emptyList()
        }
    }

    private val precipGraphMapper: Function<List<HourlyForecast>?, ForecastGraphViewModel?> = object : Function<List<HourlyForecast>?, ForecastGraphViewModel?> {
        override fun apply(input: List<HourlyForecast>?): ForecastGraphViewModel? {
            if (input != null) {
                val model = ForecastGraphViewModel()
                model.setForecastData(input, ForecastGraphViewModel.ForecastGraphType.PRECIPITATION)
                return model
            }
            return null
        }
    }

    private val forecastObserver: Observer<Forecasts> = Observer<Forecasts> { forecastData ->
        forecastGraphData.postValue(forecastMapper.apply(forecastData))
    }

    private val hrforecastObserver: Observer<List<HourlyForecast>> = Observer<List<HourlyForecast>> { forecastData ->
        hourlyForecastsData.postValue(hrForecastMapper.apply(forecastData))
        precipitationGraphData.postValue(precipGraphMapper.apply(forecastData))
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