package com.thewizrd.shared_resources.controls

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.database.WeatherDAO
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.min

@Suppress("DEPRECATION")
class ForecastsListViewModel : ViewModel() {
    private val settingsMgr = SimpleLibrary.instance.app.settingsManager

    private var locationData: LocationData? = null
    private var unitCode: String? = null
    private var localeCode: String? = null

    private var forecasts: MutableLiveData<PagedList<ForecastItemViewModel>>?
    private var hourlyForecasts: MutableLiveData<PagedList<HourlyForecastItemViewModel>>?

    private var currentForecastsData: LiveData<PagedList<ForecastItemViewModel>>? = null
    private var currentHrForecastsData: LiveData<PagedList<HourlyForecastItemViewModel>>? = null

    init {
        forecasts = MutableLiveData()
        hourlyForecasts = MutableLiveData()
    }

    fun getForecasts(): LiveData<PagedList<ForecastItemViewModel>>? {
        return forecasts
    }

    fun getHourlyForecasts(): LiveData<PagedList<HourlyForecastItemViewModel>>? {
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

                currentForecastsData?.removeObserver(forecastObserver)

                currentForecastsData = LivePagedListBuilder(
                    ForecastDataSourceFactory(
                        viewModelScope,
                        locationData!!,
                        settingsMgr.getWeatherDAO()
                    ),
                    PagedList.Config.Builder()
                        .setEnablePlaceholders(true)
                        .setPageSize(7)
                        .build()
                ).build()

                currentForecastsData!!.observeForever(forecastObserver)
                forecasts?.postValue(currentForecastsData!!.value)

                val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
                val hrFactory =
                    settingsMgr.getWeatherDAO().loadHourlyForecastsByQueryOrderByDateFilterByDate(
                        location.query,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    ).map(Function(::HourlyForecastItemViewModel))

                currentHrForecastsData?.removeObserver(hrforecastObserver)
                currentHrForecastsData = LivePagedListBuilder<Int, HourlyForecastItemViewModel>(
                    hrFactory,
                    PagedList.Config.Builder()
                        .setEnablePlaceholders(true)
                        .setPrefetchDistance(12)
                        .setPageSize(24)
                        .setMaxSize(48)
                        .build()
                ).build()
                currentHrForecastsData!!.observeForever(hrforecastObserver)
                hourlyForecasts?.postValue(currentHrForecastsData!!.value)
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsMgr.getUnitString()) ||
            !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())
        ) {
            unitCode = settingsMgr.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()

            currentForecastsData?.value?.dataSource?.invalidate()
            currentHrForecastsData?.value?.dataSource?.invalidate()
        }
    }

    private val forecastObserver =
        Observer<PagedList<ForecastItemViewModel>?> { forecastItemViewModels ->
            forecasts?.postValue(forecastItemViewModels)
        }
    private val hrforecastObserver =
        Observer<PagedList<HourlyForecastItemViewModel>?> { forecastItemViewModels ->
            hourlyForecasts?.postValue(forecastItemViewModels)
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

    private class ForecastDataSourceFactory(
        private val coroutineScope: CoroutineScope,
        private val location: LocationData,
        private val dao: WeatherDAO
    ) : DataSource.Factory<Int, ForecastItemViewModel>() {
        override fun create(): DataSource<Int, ForecastItemViewModel> {
            return ForecastDataSource(coroutineScope, location, dao)
        }
    }

    private class ForecastDataSource(
        private val coroutineScope: CoroutineScope,
        private val location: LocationData,
        private val dao: WeatherDAO
    ) : PositionalDataSource<ForecastItemViewModel>() {
        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<ForecastItemViewModel>
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                val forecasts = dao.getForecastData(location.query)

                val totalCount = forecasts?.forecast?.size ?: 0
                if (totalCount == 0) {
                    callback.onResult(emptyList(), 0, 0)
                    return@launch
                }

                val position = computeInitialLoadPosition(params, totalCount)
                val loadSize = computeInitialLoadSize(params, position, totalCount)

                callback.onResult(loadItems(forecasts, position, loadSize), position, totalCount)
            }
        }

        override fun loadRange(
            params: LoadRangeParams,
            callback: LoadRangeCallback<ForecastItemViewModel>
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                val forecasts = dao.getForecastData(location.query)

                callback.onResult(loadItems(forecasts, params.startPosition, params.loadSize))
            }
        }

        private fun loadItems(
            forecasts: Forecasts?,
            position: Int,
            loadSize: Int
        ): List<ForecastItemViewModel> {
            val totalCount = forecasts?.forecast?.size ?: 0
            if (totalCount == 0) {
                return emptyList()
            }

            val size = min(totalCount, loadSize)
            val models = ArrayList<ForecastItemViewModel>(size)
            val textForecastSize = forecasts?.txtForecast?.size ?: 0

            val isDayAndNt = textForecastSize == forecasts!!.forecast!!.size * 2
            val addTextFct = isDayAndNt || textForecastSize == forecasts.forecast.size

            for (i in position until min(totalCount, position + loadSize)) {
                val forecast = if (addTextFct) {
                    if (isDayAndNt) {
                        ForecastItemViewModel(
                            forecasts.forecast[i],
                            forecasts.txtForecast[i * 2],
                            forecasts.txtForecast[i * 2 + 1]
                        )
                    } else {
                        ForecastItemViewModel(forecasts.forecast[i], forecasts.txtForecast[i])
                    }
                } else {
                    ForecastItemViewModel(forecasts.forecast[i])
                }

                models.add(forecast)
            }

            return models
        }
    }
}