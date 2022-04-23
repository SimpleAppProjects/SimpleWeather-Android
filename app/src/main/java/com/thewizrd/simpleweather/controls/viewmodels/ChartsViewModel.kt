package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ChartsViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    private var forecastData =
        MutableLiveData<Pair<List<MinutelyForecast>?, List<HourlyForecast>?>>()

    fun getForecastData(): LiveData<Pair<List<MinutelyForecast>?, List<HourlyForecast>?>> {
        return forecastData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

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

                forecastData.postValue(
                    graphDataMapper.apply(
                        Pair(
                            currentForecastsData!!.value,
                            currentHrForecastsData!!.value
                        )
                    )
                )
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

    private val forecastObserver = Observer<Forecasts> { forecastData ->
        this.forecastData.postValue(
            graphDataMapper.apply(
                Pair(
                    forecastData,
                    currentHrForecastsData?.value
                )
            )
        )
    }

    private val hrforecastObserver = Observer<List<HourlyForecast>> { forecastData ->
        this.forecastData.postValue(
            graphDataMapper.apply(
                Pair(
                    currentForecastsData?.value,
                    forecastData
                )
            )
        )
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