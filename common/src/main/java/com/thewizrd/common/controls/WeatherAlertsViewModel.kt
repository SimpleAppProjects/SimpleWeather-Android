package com.thewizrd.common.controls

import android.app.Application
import androidx.annotation.MainThread
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlerts
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class WeatherAlertsViewModel(app: Application) : AndroidViewModel(app) {
    private var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private var alerts: MutableLiveData<List<WeatherAlertViewModel>>?

    private var currentAlertsData: LiveData<List<WeatherAlertViewModel>>? = null

    init {
        alerts = MutableLiveData()
    }

    fun getAlerts(): LiveData<List<WeatherAlertViewModel>>? {
        return alerts
    }

    @MainThread
    fun updateAlerts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                currentAlertsData?.removeObserver(alertObserver)

                val weatherAlertsLiveData = weatherDAO.getLiveWeatherAlertData(location.query)

                currentAlertsData =
                    Transformations.map(weatherAlertsLiveData) { weatherAlerts: WeatherAlerts? ->
                        val alerts: MutableList<WeatherAlertViewModel>

                        if (weatherAlerts?.alerts?.isNotEmpty() == true) {
                            alerts = ArrayList(weatherAlerts.alerts.size)
                            val now = ZonedDateTime.now()

                            for (alert in weatherAlerts.alerts) {
                                // Skip if alert has expired
                                if (!alert.expiresDate.isAfter(now) || alert.date.isAfter(now)) {
                                    continue
                                }

                                alerts.add(WeatherAlertViewModel(alert))
                            }

                            return@map alerts
                        }

                        emptyList()
                    }

                currentAlertsData!!.observeForever(alertObserver)

                alerts?.postValue(currentAlertsData!!.value)
            }
        }
    }

    private val alertObserver = Observer<List<WeatherAlertViewModel>> { alertViewModels ->
        alerts?.postValue(alertViewModels)
    }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentAlertsData?.removeObserver(alertObserver)

        currentAlertsData = null

        alerts = null
    }
}