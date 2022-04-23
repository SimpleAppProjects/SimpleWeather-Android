package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset

class AirQualityForecastViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var currentForecastsData: LiveData<Forecasts>? = null

    private var aqiForecastData = MutableLiveData<List<AirQuality>?>()

    fun getAQIForecastData(): LiveData<List<AirQuality>?> {
        return aqiForecastData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                currentForecastsData?.removeObserver(forecastObserver)
                currentForecastsData = withContext(Dispatchers.IO) {
                    weatherDAO.getLiveForecastData(location.query)
                }
                currentForecastsData!!.observeForever(forecastObserver)

                aqiForecastData.postValue(currentForecastsData?.value?.aqiForecast)
            }
        }
    }

    private val forecastObserver = Observer<Forecasts> { forecastData ->
        this.aqiForecastData.postValue(forecastData?.aqiForecast)
    }

    private val forecastMapper =
            Function<List<AirQuality>?, List<AirQualityViewModel>> { input ->
                if (input != null) {
                    val today = LocalDate.now(locationData?.tzOffset ?: ZoneOffset.systemDefault())
                    val models = ArrayList<AirQualityViewModel>(input.size)

                    for (it in input) {
                        if (!it.date.isBefore(today)) {
                            models.add(AirQualityViewModel(it))
                        }
                    }

                    return@Function models
                }

                emptyList()
            }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentForecastsData?.removeObserver(forecastObserver)
        currentForecastsData = null
    }
}