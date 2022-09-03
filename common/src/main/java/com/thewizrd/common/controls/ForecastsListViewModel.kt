package com.thewizrd.common.controls

import android.app.Application
import androidx.annotation.MainThread
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.thewizrd.shared_resources.database.WeatherDAO
import com.thewizrd.shared_resources.database.WeatherDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

class ForecastsListViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsMgr = SettingsManager(app)

    private var locationData: LocationData? = null
    private var unitCode: String? = null
    private var localeCode: String? = null

    private val weatherDAO = WeatherDatabase.getWeatherDAO(app.applicationContext)

    private val forecasts = MutableStateFlow<PagingData<ForecastItemViewModel>>(PagingData.empty())
    private val hourlyForecasts =
        MutableStateFlow<PagingData<HourlyForecastItemViewModel>>(PagingData.empty())

    private var currentForecastsData: Flow<PagingData<ForecastItemViewModel>> = emptyFlow()
    private var currentHrForecastsData: Flow<PagingData<HourlyForecastItemViewModel>> = emptyFlow()

    private var flowScope: CoroutineScope? = null

    fun getForecasts(): StateFlow<PagingData<ForecastItemViewModel>> {
        return forecasts
    }

    fun getHourlyForecasts(): StateFlow<PagingData<HourlyForecastItemViewModel>> {
        return hourlyForecasts
    }

    @MainThread
    fun updateForecasts(location: LocationData) {
        if (locationData == null || !ObjectsCompat.equals(locationData?.query, location.query)) {
            viewModelScope.launch {
                // Clone location data
                locationData = LocationQuery(location).toLocationData()

                unitCode = settingsMgr.getUnitString()
                localeCode = LocaleUtils.getLocaleCode()

                flowScope?.cancel()

                currentForecastsData = Pager(
                    config = PagingConfig(pageSize = 7)
                ) {
                    ForecastDataSource(locationData!!, weatherDAO)
                }.flow.cachedIn(viewModelScope)

                flowScope = CoroutineScope(SupervisorJob())
                flowScope?.launch {
                    currentForecastsData.collect {
                        forecasts.emit(it)
                    }
                }

                currentHrForecastsData = Pager(
                    config = PagingConfig(
                        prefetchDistance = 12,
                        pageSize = 24,
                        maxSize = 48,
                    )
                ) {
                    val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()

                    weatherDAO.loadHourlyForecastsByQueryOrderByDateFilterByDate(
                        location.query,
                        ZonedDateTime.now(location.tzOffset).minusHours((hrInterval * 0.5).toLong())
                            .truncatedTo(ChronoUnit.HOURS)
                    )
                }.flow.map { pagingData ->
                    pagingData.map {
                        HourlyForecastItemViewModel(it)
                    }
                }.cachedIn(viewModelScope)

                flowScope?.launch {
                    currentHrForecastsData.collect {
                        hourlyForecasts.emit(it)
                    }
                }
            }
        } else if (!ObjectsCompat.equals(unitCode, settingsMgr.getUnitString()) ||
            !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())
        ) {
            unitCode = settingsMgr.getUnitString()
            localeCode = LocaleUtils.getLocaleCode()

            viewModelScope.launch {
                currentForecastsData.lastOrNull()?.let {
                    forecasts.emit(it)
                }
                currentHrForecastsData.lastOrNull()?.let {
                    hourlyForecasts.emit(it)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        flowScope?.cancel()
        locationData = null
    }

    private class ForecastDataSource(
        private val location: LocationData,
        private val dao: WeatherDAO,
        private val PAGE_SIZE: Int = 7,
    ) : PagingSource<Int, ForecastItemViewModel>() {
        override fun getRefreshKey(state: PagingState<Int, ForecastItemViewModel>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ForecastItemViewModel> {
            return when (params) {
                is LoadParams.Refresh -> loadInitial(params)
                else -> loadRange(params)
            }
        }

        private suspend fun loadInitial(params: LoadParams<Int>): LoadResult<Int, ForecastItemViewModel> {
            val forecasts = dao.getForecastData(location.query)

            val totalCount = forecasts?.forecast?.size ?: 0
            if (totalCount == 0) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }

            var initialPosition = params.key ?: 0 // requestedStartPosition
            var initialLoadSize = params.loadSize // requestedLoadSize

            if (params.key != null) {
                if (params.placeholdersEnabled) {
                    // snap load size to page multiple (minimum two)
                    initialLoadSize =
                        maxOf(initialLoadSize / PAGE_SIZE, 2) * PAGE_SIZE

                    // move start so the load is centered around the key, not starting at it
                    val idealStart = initialPosition - initialLoadSize / 2
                    initialPosition = maxOf(0, idealStart / PAGE_SIZE * PAGE_SIZE)
                } else {
                    // not tiled, so don't try to snap or force multiple of a page size
                    initialPosition = maxOf(0, initialPosition - initialLoadSize / 2)
                }
            }

            var pageStart = initialPosition / PAGE_SIZE * PAGE_SIZE

            // maximum start pos is that which will encompass end of list
            val maximumLoadPage =
                (totalCount - initialLoadSize + PAGE_SIZE - 1) / PAGE_SIZE * PAGE_SIZE
            pageStart = minOf(maximumLoadPage, pageStart)

            // minimum start position is 0
            val position = maxOf(0, pageStart)
            val loadSize = minOf(totalCount - initialPosition, initialLoadSize)

            val data = loadItems(forecasts, position, loadSize)

            val nextKey = position + data.size

            return LoadResult.Page(
                data = data,
                prevKey = if (position == 0) null else position,
                nextKey = if (nextKey == totalCount) null else nextKey,
                itemsBefore = position,
                itemsAfter = totalCount - data.size - position
            )
        }

        private suspend fun loadRange(params: LoadParams<Int>): LoadResult<Int, ForecastItemViewModel> {
            var startIndex = params.key ?: 0
            var loadSize = minOf(PAGE_SIZE, params.loadSize)
            if (params is LoadParams.Prepend) {
                // clamp load size to positive indices only, and shift start index by load size
                loadSize = minOf(loadSize, startIndex)
                startIndex -= loadSize
            }

            val forecasts = dao.getForecastData(location.query)

            val data = loadItems(forecasts, startIndex, loadSize)

            // skip passing prevKey if nothing else to load. We only do this for prepend
            // direction, since 0 as first index is well defined, but max index may not be
            val prevKey = if (startIndex == 0) null else params.key
            val nextKey = startIndex + data.size

            return LoadResult.Page(
                data = data,
                prevKey = if (data.isEmpty() && params is LoadParams.Prepend) null else prevKey,
                nextKey = if (data.isEmpty() && params is LoadParams.Append) null else nextKey
            )
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