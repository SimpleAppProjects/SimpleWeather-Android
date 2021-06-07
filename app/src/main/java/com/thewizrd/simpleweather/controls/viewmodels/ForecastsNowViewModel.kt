package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ForecastsNowViewModel : ViewModel() {
    private val settingsManager = App.instance.settingsManager

    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(App.instance.appContext)

    private var forecastGraphData = MutableLiveData<RangeBarGraphViewModel>()
    private var hourlyForecastsData = MutableLiveData<List<HourlyForecastNowViewModel>>()
    private var minutelyPrecipitationGraphData = MutableLiveData<ForecastGraphViewModel>()
    private var hourlyPrecipitationGraphData = MutableLiveData<ForecastGraphViewModel>()

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    fun getForecastGraphData(): LiveData<RangeBarGraphViewModel> {
        return forecastGraphData
    }

    fun getHourlyForecastData(): LiveData<List<HourlyForecastNowViewModel>> {
        return hourlyForecastsData
    }

    fun getMinutelyPrecipitationGraphData(): LiveData<ForecastGraphViewModel> {
        return minutelyPrecipitationGraphData
    }

    fun getHourlyPrecipitationGraphData(): LiveData<ForecastGraphViewModel> {
        return hourlyPrecipitationGraphData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationData(LocationQueryViewModel(location))

                unitCode = settingsManager.getUnitString()
                localeCode = LocaleUtils.getLocaleCode()
                iconProvider = settingsManager.getIconsProvider()

                currentForecastsData?.removeObserver(forecastObserver)
                currentForecastsData = withContext(Dispatchers.IO) {
                    weatherDAO.getLiveForecastData(location.query)
                }
                currentForecastsData!!.observeForever(forecastObserver)

                forecastGraphData.postValue(forecastMapper.apply(currentForecastsData!!.value))

                currentHrForecastsData?.removeObserver(hrforecastObserver)
                currentHrForecastsData = withContext(Dispatchers.IO) {
                    val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query,
                        12,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    )
                }
                currentHrForecastsData!!.observeForever(hrforecastObserver)
                hourlyForecastsData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))

                minutelyPrecipitationGraphData.postValue(precipMinGraphMapper.apply(currentForecastsData!!.value))
                hourlyPrecipitationGraphData.postValue(precipGraphMapper.apply(currentHrForecastsData!!.value))
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            if (currentForecastsData?.value != null) {
                forecastGraphData.postValue(forecastMapper.apply(currentForecastsData!!.value))
                minutelyPrecipitationGraphData.postValue(
                    precipMinGraphMapper.apply(
                        currentForecastsData!!.value
                    )
                )
            }
            if (currentHrForecastsData?.value != null) {
                hourlyForecastsData.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
                hourlyPrecipitationGraphData.postValue(precipGraphMapper.apply(currentHrForecastsData!!.value))
            }
        }
    }

    private val forecastMapper: Function<Forecasts?, RangeBarGraphViewModel?> = Function<Forecasts?, RangeBarGraphViewModel?> { input ->
        if (input?.forecast?.size ?: 0 > 0) RangeBarGraphViewModel(input.forecast) else null
    }

    private val hrForecastMapper = Function<List<HourlyForecast>?, List<HourlyForecastNowViewModel>> { input ->
        input?.map { HourlyForecastNowViewModel(it) } ?: emptyList()
    }

    private val precipGraphMapper = Function<List<HourlyForecast>?, ForecastGraphViewModel?> { input ->
        input?.let {
            ForecastGraphViewModel().apply {
                setForecastData(it, ForecastGraphViewModel.ForecastGraphType.PRECIPITATION)
            }
        }
    }

    private val precipMinGraphMapper = Function<Forecasts?, ForecastGraphViewModel?> { input ->
        val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
        val now = ZonedDateTime.now(locationData?.tzOffset
                ?: ZoneOffset.UTC).minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS)
        input?.minForecast?.filter { it.date >= now }?.takeUnless { it.isEmpty() }?.take(60)?.let {
            ForecastGraphViewModel().apply {
                setMinutelyForecastData(it)
            }
        }
    }

    private val forecastObserver: Observer<Forecasts> = Observer<Forecasts> { forecastData ->
        forecastGraphData.postValue(forecastMapper.apply(forecastData))
        minutelyPrecipitationGraphData.postValue(precipMinGraphMapper.apply(forecastData))
    }

    private val hrforecastObserver: Observer<List<HourlyForecast>> = Observer<List<HourlyForecast>> { forecastData ->
        hourlyForecastsData.postValue(hrForecastMapper.apply(forecastData))
        hourlyPrecipitationGraphData.postValue(precipGraphMapper.apply(forecastData))
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