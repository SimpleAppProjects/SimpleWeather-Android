package com.thewizrd.simpleweather.controls.viewmodels

import androidx.annotation.MainThread
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.simpleweather.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AirQualityForecastViewModel : ViewModel() {
    private val settingsManager = App.instance.settingsManager

    var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(App.instance.appContext)

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
                locationData = LocationData(LocationQueryViewModel(location))

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

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentForecastsData?.removeObserver(forecastObserver)
        currentForecastsData = null
    }
}