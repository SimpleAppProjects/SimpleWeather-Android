package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.TextForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class WeatherExtrasViewModel {
    private List<HourlyForecastItemViewModel> hourlyForecast;
    private List<TextForecastItemViewModel> textForecast;

    private List<WeatherAlertViewModel> alerts;

    public List<HourlyForecastItemViewModel> getHourlyForecast() {
        return hourlyForecast;
    }

    public List<TextForecastItemViewModel> getTextForecast() {
        return textForecast;
    }

    public List<WeatherAlertViewModel> getAlerts() {
        return alerts;
    }

    public WeatherExtrasViewModel() {
        hourlyForecast = new ArrayList<>();
        textForecast = new ArrayList<>();
        alerts = new ArrayList<>();
    }

    public WeatherExtrasViewModel(Weather weather) {
        hourlyForecast = new ArrayList<>();
        textForecast = new ArrayList<>();
        alerts = new ArrayList<>();
        updateView(weather);
    }

    public void updateView(Weather weather) {
        // Clear all data
        clear();

        if (weather.getHrForecast() != null && weather.getHrForecast().length > 0) {
            for (final HourlyForecast hr_forecast : weather.getHrForecast()) {
                HourlyForecastItemViewModel hrforecastView;
                hrforecastView = new AsyncTask<HourlyForecastItemViewModel>().await(new Callable<HourlyForecastItemViewModel>() {
                    @Override
                    public HourlyForecastItemViewModel call() throws Exception {
                        return new HourlyForecastItemViewModel(hr_forecast);
                    }
                });
                hourlyForecast.add(hrforecastView);
            }
        }

        if (weather.getTxtForecast() != null && weather.getTxtForecast().length > 0) {
            for (final TextForecast txt_forecast : weather.getTxtForecast()) {
                TextForecastItemViewModel txtforecastView;
                txtforecastView = new AsyncTask<TextForecastItemViewModel>().await(new Callable<TextForecastItemViewModel>() {
                    @Override
                    public TextForecastItemViewModel call() throws Exception {
                        return new TextForecastItemViewModel(txt_forecast);
                    }
                });
                textForecast.add(txtforecastView);
            }
        }

        if (weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
            for (WeatherAlert alert : weather.getWeatherAlerts()) {
                // Skip if alert has expired
                if (alert.getExpiresDate().compareTo(ZonedDateTime.now()) <= 0)
                    continue;

                WeatherAlertViewModel alertView = new WeatherAlertViewModel(alert);
                alerts.add(alertView);
            }
        }
    }

    public void clear() {
        hourlyForecast.clear();
        textForecast.clear();
        alerts.clear();
    }
}
