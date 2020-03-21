package com.thewizrd.shared_resources.utils;

import android.util.Log;

import androidx.core.util.ObjectsCompat;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.database.WeatherDAO;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;

import java.util.Collection;
import java.util.concurrent.Callable;

public class ObservableForecastLoadingList<T extends BaseForecastItemViewModel> extends ObservableLoadingArrayList<T> {
    private Class<T> type;

    private Weather weather;

    public ObservableForecastLoadingList(Class<T> clazz) {
        super();
        type = clazz;
    }

    public void setWeather(Weather weather) {
        Object old = this.weather;
        this.weather = weather;

        if (ObjectsCompat.equals(old, weather) || size() == 0) {
            refresh();
        }
    }

    @Override
    public synchronized long loadMoreItems(final long count) {
        long resultCount = 0;

        if (weather != null) {
            try {
                int currentCount = size();
                _isLoading = true;

                if (type == ForecastItemViewModel.class) {
                    final WeatherDAO db = Settings.getWeatherDAO();
                    Forecasts fcast = new AsyncTask<Forecasts>().await(new Callable<Forecasts>() {
                        @Override
                        public Forecasts call() {
                            return db.getForecastData(weather.getQuery());
                        }
                    });
                    int dataCount = fcast.getForecast().size();

                    if (dataCount > 0 && dataCount != currentCount) {
                        int textForecastSize = fcast.getTxtForecast() != null ? fcast.getTxtForecast().size() : 0;

                        boolean isDayAndNt = textForecastSize == fcast.getForecast().size() * 2;
                        boolean addTextFct = isDayAndNt || textForecastSize == fcast.getForecast().size();

                        for (int i = currentCount; i < fcast.getForecast().size(); i++) {
                            final Object f;
                            if (addTextFct) {
                                if (isDayAndNt) {
                                    f = new ForecastItemViewModel(fcast.getForecast().get(i), fcast.getTxtForecast().get(i * 2), fcast.getTxtForecast().get((i * 2) + 1));
                                } else {
                                    f = new ForecastItemViewModel(fcast.getForecast().get(i), fcast.getTxtForecast().get(i));
                                }
                            } else {
                                f = new ForecastItemViewModel(fcast.getForecast().get(i));
                            }

                            synchronized (this) {
                                add((T) f);
                            }
                            resultCount++;
                        }
                    } else {
                        _hasMoreItems = false;
                    }
                } else if (type == HourlyForecastItemViewModel.class) {
                    final WeatherDAO db = Settings.getWeatherDAO();
                    int dataCount = new AsyncTask<Integer>().await(new Callable<Integer>() {
                        @Override
                        public Integer call() {
                            return db.getHourlyForecastCountByQuery(weather.getQuery());
                        }
                    });

                    if (dataCount > 0 && dataCount != currentCount) {
                        T lastItem = null;
                        if (!this.isEmpty()) {
                            lastItem = Iterables.getLast(ObservableForecastLoadingList.this);
                        }
                        Collection<HourlyForecast> data = null;

                        if (lastItem instanceof HourlyForecastItemViewModel) {
                            final HourlyForecast f = ((HourlyForecastItemViewModel) lastItem).getForecast();
                            data = new AsyncTask<Collection<HourlyForecast>>().await(new Callable<Collection<HourlyForecast>>() {
                                @Override
                                public Collection<HourlyForecast> call() {
                                    return db.loadHourlyForecastsByQueryAndDateByCount(weather.getQuery(), f.getDate(), (int) count);
                                }
                            });
                        } else {
                            data = new AsyncTask<Collection<HourlyForecast>>().await(new Callable<Collection<HourlyForecast>>() {
                                @Override
                                public Collection<HourlyForecast> call() {
                                    return db.loadHourlyForecastsByQueryByCount(weather.getQuery(), (int) count);
                                }
                            });
                        }

                        for (HourlyForecast dataItem : data) {
                            final Object fcast = new HourlyForecastItemViewModel(dataItem);
                            synchronized (this) {
                                add((T) fcast);
                            }
                            resultCount++;
                        }
                    } else {
                        _hasMoreItems = false;
                    }
                }
            } catch (Exception e) {
                Logger.writeLine(Log.ERROR, e, "Error!!");
            } finally {
                _isLoading = false;

                if (_refreshOnLoad) {
                    _refreshOnLoad = false;
                    refresh();
                }
            }
        }

        return resultCount;
    }
}
