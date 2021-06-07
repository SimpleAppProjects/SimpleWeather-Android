package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ChartsViewModel : ViewModel() {
    private val settingsManager = App.instance.settingsManager

    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(App.instance.appContext)

    private var graphModelData = MutableLiveData<List<ForecastGraphViewModel>>()

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    fun getGraphModelData(): LiveData<List<ForecastGraphViewModel>> {
        return graphModelData
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

                currentHrForecastsData?.removeObserver(hrforecastObserver)
                currentHrForecastsData = withContext(Dispatchers.IO) {
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query,
                        12,
                        ZonedDateTime.now(location.tzOffset).truncatedTo(ChronoUnit.HOURS)
                    )
                }
                currentHrForecastsData!!.observeForever(hrforecastObserver)

                currentForecastsData?.removeObserver(forecastObserver)
                currentForecastsData = withContext(Dispatchers.IO) {
                    weatherDAO.getLiveForecastData(location.query)
                }
                currentForecastsData!!.observeForever(forecastObserver)

                graphModelData.postValue(graphDataMapper.apply(Pair(currentForecastsData!!.value, currentHrForecastsData!!.value)))
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            graphModelData.postValue(graphDataMapper.apply(Pair(currentForecastsData?.value, currentHrForecastsData?.value)))
        }
    }

    private val graphDataMapper = Function<Pair<Forecasts?, List<HourlyForecast>?>, List<ForecastGraphViewModel>?> { input ->
        return@Function if (!input.first?.minForecast.isNullOrEmpty() || !input.second.isNullOrEmpty()) {
            val now = ZonedDateTime.now(locationData?.tzOffset
                    ?: ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
            createGraphModelData(input.first?.minForecast?.filter { it.date >= now }?.take(60), input.second)
        } else {
            null
        }
    }

    private fun createGraphModelData(minfcasts: List<MinutelyForecast>?, hrfcasts: List<HourlyForecast>?): List<ForecastGraphViewModel> {
        val graphTypes = ForecastGraphViewModel.ForecastGraphType.values()
        val data = ArrayList<ForecastGraphViewModel>(graphTypes.size + (if (!minfcasts.isNullOrEmpty()) 1 else 0))

        if (!minfcasts.isNullOrEmpty()) {
            data.add(ForecastGraphViewModel().apply {
                setMinutelyForecastData(minfcasts)
            })
        }

        if (!hrfcasts.isNullOrEmpty()) {
            // TODO: replace with SortedMap
            //var tempData: ForecastGraphViewModel? = null
            var popData: ForecastGraphViewModel? = null
            var windData: ForecastGraphViewModel? = null
            var rainData: ForecastGraphViewModel? = null
            var snowData: ForecastGraphViewModel? = null
            var uviData: ForecastGraphViewModel? = null
            var humidityData: ForecastGraphViewModel? = null

            for (i in hrfcasts.indices) {
                val hrfcast = hrfcasts[i]

                if (i == 0) {
                    //tempData = ForecastGraphViewModel()

                    if (hrfcasts.firstOrNull()?.extras?.pop != null || hrfcasts.lastOrNull()?.extras?.pop != null) {
                        popData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.windMph != null && hrfcasts.firstOrNull()?.windKph != null ||
                            hrfcasts.lastOrNull()?.windMph != null && hrfcasts.lastOrNull()?.windKph != null) {
                        windData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfRainIn != null && hrfcasts.firstOrNull()?.extras?.qpfRainMm != null ||
                            hrfcasts.lastOrNull()?.extras?.qpfRainIn != null && hrfcasts.lastOrNull()?.extras?.qpfRainMm != null) {
                        rainData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.qpfSnowIn != null && hrfcasts.firstOrNull()?.extras?.qpfSnowCm != null ||
                            hrfcasts.lastOrNull()?.extras?.qpfSnowIn != null && hrfcasts.lastOrNull()?.extras?.qpfSnowCm != null) {
                        snowData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.uvIndex != null || hrfcasts.lastOrNull()?.extras?.uvIndex != null) {
                        uviData = ForecastGraphViewModel()
                    }
                    if (hrfcasts.firstOrNull()?.extras?.humidity != null || hrfcasts.lastOrNull()?.extras?.humidity != null) {
                        humidityData = ForecastGraphViewModel()
                    }
                }

                //tempData?.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.TEMPERATURE)
                if (popData != null) {
                    if (hrfcast.extras?.pop != null) {
                        popData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.PRECIPITATION)
                    }
                }
                if (windData != null) {
                    if (hrfcast.windMph != null && hrfcast.windKph != null) {
                        windData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.WIND)
                    }
                }
                if (rainData != null) {
                    if (hrfcast.extras?.qpfRainIn != null && hrfcast.extras?.qpfRainMm != null) {
                        rainData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.RAIN)
                    }
                }
                if (snowData != null) {
                    if (hrfcast.extras?.qpfSnowIn != null && hrfcast.extras?.qpfSnowCm != null) {
                        snowData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.SNOW)
                    }
                }
                if (uviData != null) {
                    if (hrfcast.extras?.uvIndex != null) {
                        uviData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.UVINDEX)
                    }
                }
                if (humidityData != null) {
                    if (hrfcast.extras?.humidity != null) {
                        humidityData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.HUMIDITY)
                    }
                }
            }

            /*
            if (tempData?.seriesData?.size ?: 0 > 0) {
                data.add(tempData!!)
            }
             */
            if (popData?.seriesData?.size ?: 0 > 0) {
                data.add(popData!!)
            }
            if (windData?.seriesData?.size ?: 0 > 0) {
                data.add(windData!!)
            }
            if (humidityData?.seriesData?.size ?: 0 > 0) {
                data.add(humidityData!!)
            }
            if (uviData?.seriesData?.size ?: 0 > 0) {
                data.add(uviData!!)
            }
            if (rainData?.seriesData?.size ?: 0 > 0) {
                data.add(rainData!!)
            }
            if (snowData?.seriesData?.size ?: 0 > 0) {
                data.add(snowData!!)
            }
        }

        return data
    }

    private val forecastObserver = Observer<Forecasts> { forecastData ->
        graphModelData.postValue(graphDataMapper.apply(Pair(forecastData, currentHrForecastsData?.value)))
    }

    private val hrforecastObserver = Observer<List<HourlyForecast>> { forecastData ->
        graphModelData.postValue(graphDataMapper.apply(Pair(currentForecastsData?.value, forecastData)))
    }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentHrForecastsData?.removeObserver(hrforecastObserver)
        currentHrForecastsData = null

        currentForecastsData?.removeObserver(forecastObserver)
        currentForecastsData = null
    }
}