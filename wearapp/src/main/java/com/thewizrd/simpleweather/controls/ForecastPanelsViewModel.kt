package com.thewizrd.simpleweather.controls

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.thewizrd.shared_resources.controls.ForecastItemViewModel
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.App
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class ForecastPanelsViewModel : ViewModel() {
    private val settingsMgr = App.instance.settingsManager

    private var locationData: LocationData? = null
    private var unitCode: String? = null
    private var localeCode: String? = null
    private var iconProvider: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(App.instance.appContext)

    private var forecasts: MutableLiveData<List<ForecastItemViewModel>>?
    private var hourlyForecasts: MutableLiveData<List<HourlyForecastItemViewModel>>?

    private var currentForecastsData: LiveData<Forecasts>? = null
    private var currentHrForecastsData: LiveData<List<HourlyForecast>>? = null

    init {
        forecasts = MutableLiveData()
        hourlyForecasts = MutableLiveData()
    }

    fun getForecasts(): LiveData<List<ForecastItemViewModel>>? {
        return forecasts
    }

    fun getHourlyForecasts(): LiveData<List<HourlyForecastItemViewModel>>? {
        return hourlyForecasts
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationData(LocationQueryViewModel(location))

                unitCode = settingsMgr.getUnitString()
                localeCode = LocaleUtils.getLocaleCode()
                iconProvider = settingsMgr.getIconsProvider()

                currentForecastsData?.removeObserver(forecastObserver)
                currentForecastsData = weatherDAO.getLiveForecastData(location.query)

                currentForecastsData!!.observeForever(forecastObserver)
                forecasts?.postValue(forecastMapper.apply(currentForecastsData!!.getValue()))

                currentHrForecastsData?.removeObserver(hrforecastObserver)
                val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
                currentHrForecastsData =
                    weatherDAO.getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                        location.query, 6,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    )
                currentHrForecastsData!!.observeForever(hrforecastObserver)
                hourlyForecasts?.postValue(hrForecastMapper.apply(currentHrForecastsData!!.getValue()))
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsMgr.getUnitString()) ||
            !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode()) ||
            !ObjectsCompat.equals(iconProvider, settingsMgr.getIconsProvider())
        ) {
            unitCode = settingsMgr.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()
            iconProvider = settingsMgr.getIconsProvider()

            if (currentForecastsData?.value != null) {
                forecasts?.postValue(forecastMapper.apply(currentForecastsData!!.value))
            }
            if (currentHrForecastsData?.value != null) {
                hourlyForecasts?.postValue(hrForecastMapper.apply(currentHrForecastsData!!.value))
            }
        }
    }

    private val forecastMapper = Function<Forecasts?, List<ForecastItemViewModel>> { input ->
        if (input?.forecast?.isNotEmpty() == true) {
            val totalCount = input.forecast.size
            val models = ArrayList<ForecastItemViewModel>(totalCount)

            for (i in 0 until Math.min(totalCount, 4)) {
                models.add(ForecastItemViewModel(input.forecast[i]))
            }

            return@Function models
        }

        emptyList()
    }

    private val hrForecastMapper =
        Function<List<HourlyForecast>?, List<HourlyForecastItemViewModel>> { input ->
            if (input != null) {
                val models = ArrayList<HourlyForecastItemViewModel>(input.size)

                for (fcast in input) {
                    models.add(HourlyForecastItemViewModel(fcast))
                }

                return@Function models
            }

            emptyList()
        }

    private val forecastObserver = Observer<Forecasts?> { forecastData ->
        forecasts?.postValue(forecastMapper.apply(forecastData))
    }

    private val hrforecastObserver = Observer<List<HourlyForecast>?> { forecastData ->
        hourlyForecasts?.postValue(hrForecastMapper.apply(forecastData))
    }

    override fun onCleared() {
        super.onCleared()

        locationData = null

        currentForecastsData?.removeObserver(forecastObserver)
        currentHrForecastsData?.removeObserver(hrforecastObserver)

        currentForecastsData = null
        currentHrForecastsData = null

        forecasts = null
        hourlyForecasts = null
    }
}