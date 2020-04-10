package com.thewizrd.simpleweather.controls;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastGraphViewModel extends ViewModel {
    private Handler mMainHandler;
    private LocationData location;

    private MutableLiveData<List<ForecastItemViewModel>> forecasts;
    private MutableLiveData<List<HourlyForecastItemViewModel>> hourlyForecasts;

    private LiveData<List<ForecastItemViewModel>> currentForecastsData;
    private LiveData<List<HourlyForecastItemViewModel>> currentHrForecastsData;

    public ForecastGraphViewModel() {
        forecasts = new MutableLiveData<>();
        hourlyForecasts = new MutableLiveData<>();

        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<List<ForecastItemViewModel>> getForecasts() {
        return forecasts;
    }

    public LiveData<List<HourlyForecastItemViewModel>> getHourlyForecasts() {
        return hourlyForecasts;
    }

    public void updateForecasts(@NonNull LocationData location) {
        if (!ObjectsCompat.equals(this.location, location)) {
            this.location = location;

            currentForecastsData = Transformations.map(Settings.getWeatherDAO().getLiveForecastData(location.getQuery()),
                    new Function<Forecasts, List<ForecastItemViewModel>>() {
                        @Override
                        public List<ForecastItemViewModel> apply(Forecasts input) {
                            if (input != null && input.getForecast() != null) {
                                final int totalCount = input.getForecast().size();
                                final List<ForecastItemViewModel> models = new ArrayList<>(totalCount);

                                int textForecastSize = input.getTxtForecast() != null ? input.getTxtForecast().size() : 0;

                                boolean isDayAndNt = textForecastSize == input.getForecast().size() * 2;
                                boolean addTextFct = isDayAndNt || textForecastSize == input.getForecast().size();

                                for (int i = 0; i < Math.min(totalCount, 10); i++) {
                                    ForecastItemViewModel forecast;
                                    if (addTextFct) {
                                        if (isDayAndNt) {
                                            forecast = new ForecastItemViewModel(input.getForecast().get(i), input.getTxtForecast().get(i * 2), input.getTxtForecast().get((i * 2) + 1));
                                        } else {
                                            forecast = new ForecastItemViewModel(input.getForecast().get(i), input.getTxtForecast().get(i));
                                        }
                                    } else {
                                        forecast = new ForecastItemViewModel(input.getForecast().get(i));
                                    }

                                    models.add(forecast);
                                }

                                return models;
                            }

                            return Collections.emptyList();
                        }
                    });
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    currentForecastsData.removeObserver(forecastObserver);
                    currentForecastsData.observeForever(forecastObserver);
                }
            });
            forecasts.postValue(currentForecastsData.getValue());

            currentHrForecastsData = Transformations.map(Settings.getWeatherDAO().getLiveHourlyForecastsByQueryOrderByDateByLimit(location.getQuery(), 24),
                    new Function<List<HourlyForecast>, List<HourlyForecastItemViewModel>>() {
                        @Override
                        public List<HourlyForecastItemViewModel> apply(List<HourlyForecast> input) {
                            if (input != null) {
                                final List<HourlyForecastItemViewModel> models = new ArrayList<>(input.size());

                                for (HourlyForecast fcast : input) {
                                    models.add(new HourlyForecastItemViewModel(fcast));
                                }

                                return models;
                            }

                            return Collections.emptyList();
                        }
                    });
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

    private Observer<List<ForecastItemViewModel>> forecastObserver = new Observer<List<ForecastItemViewModel>>() {
        @Override
        public void onChanged(List<ForecastItemViewModel> forecastItemViewModels) {
            forecasts.setValue(forecastItemViewModels);
        }
    };

    private Observer<List<HourlyForecastItemViewModel>> hrforecastObserver = new Observer<List<HourlyForecastItemViewModel>>() {
        @Override
        public void onChanged(List<HourlyForecastItemViewModel> forecastItemViewModels) {
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
}