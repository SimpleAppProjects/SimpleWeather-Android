package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ChartsViewModel : ViewModel() {
    private val settingsManager = App.instance.settingsManager

    var locationData: LocationData? = null
    var unitCode: String? = null
    var localeCode: String? = null
    var iconProvider: String? = null

    private var graphModelData = MutableLiveData<List<ForecastGraphViewModel>>()

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
                    settingsManager.getWeatherDAO().getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(location.query, 12, ZonedDateTime.now(location.tzOffset).truncatedTo(ChronoUnit.HOURS))
                }
                currentHrForecastsData!!.observeForever(hrforecastObserver)
                graphModelData.postValue(graphDataMapper.apply(currentHrForecastsData!!.value))
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsManager.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, settingsManager.getIconsProvider())) {
            unitCode = settingsManager.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsManager.getIconsProvider()

            if (currentHrForecastsData?.value != null) {
                graphModelData.postValue(graphDataMapper.apply(currentHrForecastsData!!.value))
            }
        }
    }

    private val graphDataMapper: Function<List<HourlyForecast>?, List<ForecastGraphViewModel>?> = object : Function<List<HourlyForecast>?, List<ForecastGraphViewModel>?> {
        override fun apply(input: List<HourlyForecast>?): List<ForecastGraphViewModel>? {
            if (input?.size ?: 0 > 0) {
                return createGraphModelData(input!!)
            }
            return null
        }
    }

    private fun createGraphModelData(hrfcasts: List<HourlyForecast>): List<ForecastGraphViewModel> {
        val graphTypes = ForecastGraphViewModel.ForecastGraphType.values()
        val data = ArrayList<ForecastGraphViewModel>(graphTypes.size)

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

                if (hrfcast.extras?.pop != null) {
                    popData = ForecastGraphViewModel()
                }
                if (hrfcast.windMph != null && hrfcast.windKph != null) {
                    windData = ForecastGraphViewModel()
                }
                if (hrfcast.extras?.qpfRainIn != null && hrfcast.extras?.qpfRainMm != null) {
                    rainData = ForecastGraphViewModel()
                }
                if (hrfcast.extras?.qpfSnowIn != null && hrfcast.extras?.qpfSnowCm != null) {
                    snowData = ForecastGraphViewModel()
                }
                if (hrfcast.extras?.uvIndex != null) {
                    uviData = ForecastGraphViewModel()
                }
                if (hrfcast.extras?.humidity != null) {
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

        return data
    }

    private val hrforecastObserver: Observer<List<HourlyForecast>> = Observer<List<HourlyForecast>> { forecastData ->
        graphModelData.postValue(graphDataMapper.apply(forecastData))
    }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentHrForecastsData?.removeObserver(hrforecastObserver)

        currentHrForecastsData = null
    }
}