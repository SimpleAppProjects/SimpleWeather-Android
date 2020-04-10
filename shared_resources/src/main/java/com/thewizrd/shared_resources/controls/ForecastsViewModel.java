package com.thewizrd.shared_resources.controls;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
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

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.database.WeatherDAO;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class ForecastsViewModel extends ViewModel {
    private Handler mMainHandler;
    private LocationData location;

    private MutableLiveData<PagedList<ForecastItemViewModel>> forecasts;
    private MutableLiveData<PagedList<HourlyForecastItemViewModel>> hourlyForecasts;

    private LiveData<PagedList<ForecastItemViewModel>> currentForecastsData;
    private LiveData<PagedList<HourlyForecastItemViewModel>> currentHrForecastsData;

    public ForecastsViewModel() {
        forecasts = new MutableLiveData<>();
        hourlyForecasts = new MutableLiveData<>();

        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<PagedList<ForecastItemViewModel>> getForecasts() {
        return forecasts;
    }

    public LiveData<PagedList<HourlyForecastItemViewModel>> getHourlyForecasts() {
        return hourlyForecasts;
    }

    public void updateForecasts(@NonNull LocationData location) {
        if (!ObjectsCompat.equals(this.location, location)) {
            this.location = location;

            currentForecastsData = new LivePagedListBuilder<>(
                    new ForecastDataSourceFactory(location, Settings.getWeatherDAO()),
                    new PagedList.Config.Builder()
                            .setEnablePlaceholders(true)
                            .setPageSize(7)
                            .build())
                    .build();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    currentForecastsData.removeObserver(forecastObserver);
                    currentForecastsData.observeForever(forecastObserver);
                }
            });
            forecasts.postValue(currentForecastsData.getValue());

            DataSource.Factory<Integer, HourlyForecastItemViewModel> hrFactory = Settings.getWeatherDAO().loadHourlyForecastsByQueryOrderByDate(location.getQuery())
                    .map(new Function<HourlyForecast, HourlyForecastItemViewModel>() {
                        @Override
                        public HourlyForecastItemViewModel apply(HourlyForecast input) {
                            return new HourlyForecastItemViewModel(input);
                        }
                    });

            currentHrForecastsData = new LivePagedListBuilder<>(
                    hrFactory,
                    new PagedList.Config.Builder()
                            .setEnablePlaceholders(true)
                            .setPrefetchDistance(12)
                            .setPageSize(24)
                            .setMaxSize(48)
                            .build())
                    .build();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    currentHrForecastsData.removeObserver(hrforecastObserver);
                    currentHrForecastsData.observeForever(hrforecastObserver);
                }
            });
            hourlyForecasts.postValue(currentHrForecastsData.getValue());
        }
    }

    private Observer<PagedList<ForecastItemViewModel>> forecastObserver = new Observer<PagedList<ForecastItemViewModel>>() {
        @Override
        public void onChanged(PagedList<ForecastItemViewModel> forecastItemViewModels) {
            forecasts.setValue(forecastItemViewModels);
        }
    };

    private Observer<PagedList<HourlyForecastItemViewModel>> hrforecastObserver = new Observer<PagedList<HourlyForecastItemViewModel>>() {
        @Override
        public void onChanged(PagedList<HourlyForecastItemViewModel> forecastItemViewModels) {
            hourlyForecasts.setValue(forecastItemViewModels);
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();

        location = null;

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
            Forecasts forecasts = new AsyncTask<Forecasts>().await(new Callable<Forecasts>() {
                @Override
                public Forecasts call() {
                    return dao.getForecastData(location.getQuery());
                }
            });

            int totalCount = forecasts.getForecast().size();
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
            Forecasts forecasts = new AsyncTask<Forecasts>().await(new Callable<Forecasts>() {
                @Override
                public Forecasts call() {
                    return dao.getForecastData(location.getQuery());
                }
            });

            callback.onResult(loadItems(forecasts, params.startPosition, params.loadSize));
        }

        private List<ForecastItemViewModel> loadItems(Forecasts forecasts, int position, int loadSize) {
            final int totalCount = forecasts.getForecast().size();
            final List<ForecastItemViewModel> models = new ArrayList<>();

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
