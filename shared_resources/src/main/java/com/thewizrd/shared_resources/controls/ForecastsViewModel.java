package com.thewizrd.shared_resources.controls;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;

import com.thewizrd.shared_resources.database.WeatherDAO;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class ForecastsViewModel extends ViewModel {
    private LocationData locationData;
    private String unitCode;
    private String localeCode;

    private MutableLiveData<PagedList<ForecastItemViewModel>> forecasts;
    private MutableLiveData<PagedList<HourlyForecastItemViewModel>> hourlyForecasts;

    private LiveData<PagedList<ForecastItemViewModel>> currentForecastsData;
    private LiveData<PagedList<HourlyForecastItemViewModel>> currentHrForecastsData;

    public ForecastsViewModel() {
        forecasts = new MutableLiveData<>();
        hourlyForecasts = new MutableLiveData<>();
    }

    public LiveData<PagedList<ForecastItemViewModel>> getForecasts() {
        return forecasts;
    }

    public LiveData<PagedList<HourlyForecastItemViewModel>> getHourlyForecasts() {
        return hourlyForecasts;
    }

    @MainThread
    public void updateForecasts(@NonNull LocationData location) {
        if (this.locationData == null || !ObjectsCompat.equals(this.locationData.getQuery(), location.getQuery())) {
            // Clone location data
            this.locationData = new LocationData(new LocationQueryViewModel(location));

            unitCode = Settings.getUnitString();
            localeCode = LocaleUtils.getLocaleCode();

            if (currentForecastsData != null) {
                currentForecastsData.removeObserver(forecastObserver);
            }

            currentForecastsData = new LivePagedListBuilder<>(
                    new ForecastDataSourceFactory(locationData, Settings.getWeatherDAO()),
                    new PagedList.Config.Builder()
                            .setEnablePlaceholders(true)
                            .setPageSize(7)
                            .build())
                    .build();

            currentForecastsData.observeForever(forecastObserver);
            if (forecasts != null)
                forecasts.postValue(currentForecastsData.getValue());

            DataSource.Factory<Integer, HourlyForecastItemViewModel> hrFactory = Settings.getWeatherDAO().loadHourlyForecastsByQueryOrderByDateFilterByDate(location.getQuery(), ZonedDateTime.now(location.getTzOffset()).truncatedTo(ChronoUnit.HOURS))
                    .map(new Function<HourlyForecast, HourlyForecastItemViewModel>() {
                        @Override
                        public HourlyForecastItemViewModel apply(HourlyForecast input) {
                            return new HourlyForecastItemViewModel(input);
                        }
                    });

            if (currentHrForecastsData != null) {
                currentHrForecastsData.removeObserver(hrforecastObserver);
            }
            currentHrForecastsData = new LivePagedListBuilder<>(
                    hrFactory,
                    new PagedList.Config.Builder()
                            .setEnablePlaceholders(true)
                            .setPrefetchDistance(12)
                            .setPageSize(24)
                            .setMaxSize(48)
                            .build())
                    .build();
            currentHrForecastsData.observeForever(hrforecastObserver);
            if (hourlyForecasts != null)
                hourlyForecasts.postValue(currentHrForecastsData.getValue());
        } else if (!ObjectsCompat.equals(unitCode, Settings.getUnitString()) || !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())) {
            unitCode = Settings.getUnitString();
            localeCode = LocaleUtils.getLocaleCode();

            if (currentForecastsData != null && currentForecastsData.getValue() != null) {
                currentForecastsData.getValue().getDataSource().invalidate();
            }
            if (currentHrForecastsData != null && currentHrForecastsData.getValue() != null) {
                currentHrForecastsData.getValue().getDataSource().invalidate();
            }
        }
    }

    private Observer<PagedList<ForecastItemViewModel>> forecastObserver = new Observer<PagedList<ForecastItemViewModel>>() {
        @Override
        public void onChanged(PagedList<ForecastItemViewModel> forecastItemViewModels) {
            if (forecasts != null) {
                forecasts.postValue(forecastItemViewModels);
            }
        }
    };

    private Observer<PagedList<HourlyForecastItemViewModel>> hrforecastObserver = new Observer<PagedList<HourlyForecastItemViewModel>>() {
        @Override
        public void onChanged(PagedList<HourlyForecastItemViewModel> forecastItemViewModels) {
            if (hourlyForecasts != null) {
                hourlyForecasts.postValue(forecastItemViewModels);
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();

        locationData = null;

        if (currentForecastsData != null)
            currentForecastsData.removeObserver(forecastObserver);
        if (currentHrForecastsData != null)
            currentHrForecastsData.removeObserver(hrforecastObserver);

        currentForecastsData = null;
        currentHrForecastsData = null;

        forecasts = null;
        hourlyForecasts = null;
    }

    private static class ForecastDataSourceFactory extends DataSource.Factory<Integer, ForecastItemViewModel> {
        private final LocationData location;
        private final WeatherDAO dao;

        ForecastDataSourceFactory(@NonNull LocationData location, @NonNull WeatherDAO dao) {
            this.location = location;
            this.dao = dao;
        }

        @NonNull
        @Override
        public DataSource<Integer, ForecastItemViewModel> create() {
            return new ForecastDataSource(location, dao);
        }
    }

    private static class ForecastDataSource extends PositionalDataSource<ForecastItemViewModel> {
        private final LocationData location;
        private final WeatherDAO dao;

        ForecastDataSource(@NonNull LocationData location, @NonNull WeatherDAO dao) {
            this.location = location;
            this.dao = dao;
        }

        @Override
        public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<ForecastItemViewModel> callback) {
            Forecasts forecasts = AsyncTask.await(new Callable<Forecasts>() {
                @Override
                public Forecasts call() {
                    return dao.getForecastData(location.getQuery());
                }
            });

            int totalCount = forecasts != null && forecasts.getForecast() != null ? forecasts.getForecast().size() : 0;
            if (totalCount == 0) {
                callback.onResult(Collections.<ForecastItemViewModel>emptyList(), 0, 0);
                return;
            }

            final int position = computeInitialLoadPosition(params, totalCount);
            final int loadSize = computeInitialLoadSize(params, position, totalCount);

            callback.onResult(loadItems(forecasts, position, loadSize), position, totalCount);
        }

        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<ForecastItemViewModel> callback) {
            Forecasts forecasts = AsyncTask.await(new Callable<Forecasts>() {
                @Override
                public Forecasts call() {
                    return dao.getForecastData(location.getQuery());
                }
            });

            callback.onResult(loadItems(forecasts, params.startPosition, params.loadSize));
        }

        private List<ForecastItemViewModel> loadItems(@Nullable Forecasts forecasts, int position, int loadSize) {
            final int totalCount = forecasts != null && forecasts.getForecast() != null ? forecasts.getForecast().size() : 0;
            if (totalCount == 0) {
                return Collections.emptyList();
            }

            final int size = Math.min(totalCount, loadSize);
            final List<ForecastItemViewModel> models = new ArrayList<>(size);
            int textForecastSize = forecasts.getTxtForecast() != null ? forecasts.getTxtForecast().size() : 0;

            boolean isDayAndNt = textForecastSize == forecasts.getForecast().size() * 2;
            boolean addTextFct = isDayAndNt || textForecastSize == forecasts.getForecast().size();

            for (int i = position; i < Math.min(totalCount, position + loadSize); i++) {
                ForecastItemViewModel forecast;
                if (addTextFct) {
                    if (isDayAndNt) {
                        forecast = new ForecastItemViewModel(forecasts.getForecast().get(i), forecasts.getTxtForecast().get(i * 2), forecasts.getTxtForecast().get((i * 2) + 1));
                    } else {
                        forecast = new ForecastItemViewModel(forecasts.getForecast().get(i), forecasts.getTxtForecast().get(i));
                    }
                } else {
                    forecast = new ForecastItemViewModel(forecasts.getForecast().get(i));
                }

                models.add(forecast);
            }

            return models;
        }
    }
}
