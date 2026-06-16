package com.thewizrd.simpleweather.controls.viewmodels

import android.app.Application
import androidx.annotation.MainThread
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class AirQualityForecastViewModel(app: Application) : AndroidViewModel(app) {
    var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var currentForecastsData: Flow<Forecasts> = emptyFlow()

    private var aqiForecastData = MutableStateFlow<List<AirQuality>?>(null)

    private var flowScope: CoroutineScope? = null

    fun getAQIForecastData(): StateFlow<List<AirQuality>?> {
        return aqiForecastData
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                flowScope?.cancel()

                currentForecastsData =
                    weatherDAO.getLiveForecastData(location.query).filterNotNull()
                        .distinctUntilChanged()

                flowScope = CoroutineScope(SupervisorJob())
                flowScope?.launch {
                    currentForecastsData.collect {
                        aqiForecastData.emit(it.aqiForecast)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        flowScope?.cancel()
        locationData = null
    }
}