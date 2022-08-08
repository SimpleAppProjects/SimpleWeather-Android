package com.thewizrd.common.controls

import android.app.Application
import androidx.annotation.MainThread
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class WeatherAlertsViewModel(app: Application) : AndroidViewModel(app) {
    private var locationData: LocationData? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private val alerts = MutableStateFlow<List<WeatherAlertViewModel>>(emptyList())
    private var currentAlertsData: Flow<List<WeatherAlertViewModel>> = emptyFlow()

    private var flowScope: CoroutineScope? = null

    fun getAlerts(): StateFlow<List<WeatherAlertViewModel>> {
        return alerts
    }

    @MainThread
    fun updateAlerts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                flowScope?.cancel()

                currentAlertsData =
                    weatherDAO.getLiveWeatherAlertData(location.query).asFlow().map {
                        return@map it?.alerts?.let { weatherAlerts ->
                            val alerts = ArrayList<WeatherAlertViewModel>(weatherAlerts.size)
                            val now = ZonedDateTime.now()

                            for (alert in weatherAlerts) {
                                // Skip if alert has expired
                                if (!alert.expiresDate.isAfter(now) || alert.date.isAfter(now)) {
                                    continue
                                }

                                alerts.add(WeatherAlertViewModel(alert))
                            }

                            alerts
                        } ?: emptyList()
                    }

                flowScope = CoroutineScope(SupervisorJob())
                flowScope?.launch {
                    currentAlertsData.collect {
                        alerts.emit(it)
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