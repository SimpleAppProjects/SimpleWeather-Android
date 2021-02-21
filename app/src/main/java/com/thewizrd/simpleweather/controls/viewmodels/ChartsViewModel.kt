package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.tasks.AsyncTask
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.shared_resources.weatherdata.HourlyForecast
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Callable

class ChartsViewModel : ViewModel() {
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
            // Clone location data
            locationData = LocationData(LocationQueryViewModel(location))

            unitCode = Settings.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = Settings.getIconsProvider()

            currentHrForecastsData?.removeObserver(hrforecastObserver)
            currentHrForecastsData = AsyncTask.await(Callable<LiveData<List<HourlyForecast>>?> {
                Settings.getWeatherDAO().getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(location.query, 12, ZonedDateTime.now(location.tzOffset).truncatedTo(ChronoUnit.HOURS))
            })
            currentHrForecastsData!!.observeForever(hrforecastObserver)
            graphModelData.postValue(graphDataMapper.apply(currentHrForecastsData!!.value))
        } else if (!ObjectsCompat.equals(unitCode, Settings.getUnitString()) ||
                !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
                !ObjectsCompat.equals(iconProvider, Settings.getIconsProvider())) {
            unitCode = Settings.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = Settings.getIconsProvider()

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
                if (hrfcast.extras?.pop ?: -1 >= 0) {
                    popData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.PRECIPITATION)
                }
            }
            if (windData != null) {
                if (hrfcast.windMph ?: -1f >= 0) {
                    windData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.WIND)
                }
            }
            if (rainData != null) {
                if (hrfcast.extras?.qpfRainIn ?: -1f >= 0 && hrfcast.extras?.qpfRainMm ?: -1f >= 0) {
                    rainData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.RAIN)
                }
            }
            if (snowData != null) {
                if (hrfcast.extras?.qpfSnowIn ?: -1f >= 0 && hrfcast.extras?.qpfSnowCm ?: -1f >= 0) {
                    snowData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.SNOW)
                }
            }
            if (uviData != null) {
                if (hrfcast.extras?.uvIndex ?: -1f >= 0) {
                    uviData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.UVINDEX)
                }
            }
            if (humidityData != null) {
                if (hrfcast.extras?.humidity ?: -1 >= 0) {
                    humidityData.addForecastData(hrfcast, ForecastGraphViewModel.ForecastGraphType.HUMIDITY)
                }
            }
        }

        /*
        if (tempData != null) {
            data.add(tempData)
        }
         */
        if (popData != null) {
            data.add(popData)
        }
        if (windData != null) {
            data.add(windData)
        }
        if (humidityData != null) {
            data.add(humidityData)
        }
        if (uviData != null) {
            data.add(uviData)
        }
        if (rainData != null) {
            data.add(rainData)
        }
        if (snowData != null) {
            data.add(snowData)
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