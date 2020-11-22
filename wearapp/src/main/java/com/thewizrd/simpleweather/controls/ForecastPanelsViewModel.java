package com.thewizrd.simpleweather.controls;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForecastPanelsViewModel extends ViewModel {
    private LocationData locationData;
    private String unitCode;
    private String localeCode;

    private MutableLiveData<List<ForecastItemViewModel>> forecasts;
    private MutableLiveData<List<HourlyForecastItemViewModel>> hourlyForecasts;

    private LiveData<Forecasts> currentForecastsData;
    private LiveData<List<HourlyForecast>> currentHrForecastsData;

    public ForecastPanelsViewModel() {
        forecasts = new MutableLiveData<>();
        hourlyForecasts = new MutableLiveData<>();
    }

    public LiveData<List<ForecastItemViewModel>> getForecasts() {
        return forecasts;
    }

    public LiveData<List<HourlyForecastItemViewModel>> getHourlyForecasts() {
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
            currentForecastsData = Settings.getWeatherDAO().getLiveForecastData(location.getQuery());

            currentForecastsData.observeForever(forecastObserver);
            if (forecasts != null)
                forecasts.postValue(forecastMapper.apply(currentForecastsData.getValue()));

            if (currentHrForecastsData != null) {
                currentHrForecastsData.removeObserver(hrforecastObserver);
            }
            currentHrForecastsData = Settings.getWeatherDAO().getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(location.getQuery(), 6, ZonedDateTime.now(location.getTzOffset()).truncatedTo(ChronoUnit.HOURS));
            currentHrForecastsData.observeForever(hrforecastObserver);
            if (hourlyForecasts != null)
                hourlyForecasts.postValue(hrForecastMapper.apply(currentHrForecastsData.getValue()));
        } else if (!ObjectsCompat.equals(unitCode, Settings.getUnitString()) || !ObjectsCompat.equals(localeCode, LocaleUtils.getLocaleCode())) {
            unitCode = Settings.getUnitString();
            localeCode = LocaleUtils.getLocaleCode();

            if (currentForecastsData != null && currentForecastsData.getValue() != null) {
                forecasts.postValue(forecastMapper.apply(currentForecastsData.getValue()));
            }
            if (currentHrForecastsData != null && currentHrForecastsData.getValue() != null) {
                hourlyForecasts.postValue(hrForecastMapper.apply(currentHrForecastsData.getValue()));
            }
        }
    }

    private Function<Forecasts, List<ForecastItemViewModel>> forecastMapper = new Function<Forecasts, List<ForecastItemViewModel>>() {
        @Override
        public List<ForecastItemViewModel> apply(Forecasts input) {
            if (input != null && input.getForecast() != null) {
                final int totalCount = input.getForecast().size();
                final List<ForecastItemViewModel> models = new ArrayList<>(totalCount);

                for (int i = 0; i < Math.min(totalCount, 4); i++) {
                    models.add(new ForecastItemViewModel(input.getForecast().get(i)));
                }

                return models;
            }

            return Collections.emptyList();
        }
    };

    private Function<List<HourlyForecast>, List<HourlyForecastItemViewModel>> hrForecastMapper = new Function<List<HourlyForecast>, List<HourlyForecastItemViewModel>>() {
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
    };

    private Observer<Forecasts> forecastObserver = new Observer<Forecasts>() {
        @Override
        public void onChanged(Forecasts forecastData) {
            if (forecasts != null) {
                forecasts.postValue(forecastMapper.apply(forecastData));
            }
        }
    };

    private Observer<List<HourlyForecast>> hrforecastObserver = new Observer<List<HourlyForecast>>() {
        @Override
        public void onChanged(List<HourlyForecast> forecastData) {
            if (hourlyForecasts != null) {
                hourlyForecasts.postValue(hrForecastMapper.apply(forecastData));
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
}