package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.TextForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherExtrasViewModel {
    private List<HourlyForecastItemViewModel> hourlyForecast;
    private List<TextForecastItemViewModel> textForecast;

    private String chance;
    private String qpfRain;
    private String qpfSnow;

    private List<WeatherAlertViewModel> alerts;

    public List<HourlyForecastItemViewModel> getHourlyForecast() {
        return hourlyForecast;
    }

    public List<TextForecastItemViewModel> getTextForecast() {
        return textForecast;
    }

    public String getChance() {
        return chance;
    }

    public String getQpfRain() {
        return qpfRain;
    }

    public String getQpfSnow() {
        return qpfSnow;
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
            for (HourlyForecast hr_forecast : weather.getHrForecast()) {
                HourlyForecastItemViewModel hrforecastView = new HourlyForecastItemViewModel(hr_forecast);
                hourlyForecast.add(hrforecastView);
            }
        }

        if (weather.getTxtForecast() != null && weather.getTxtForecast().length > 0) {
            for (TextForecast txt_forecast : weather.getTxtForecast()) {
                TextForecastItemViewModel txtforecastView = new TextForecastItemViewModel(txt_forecast);
                textForecast.add(txtforecastView);
            }
        }

        if (weather.getPrecipitation() != null) {
            chance = weather.getPrecipitation().getPop() + "%";
            qpfRain = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfRainIn()) :
                    String.format(Locale.getDefault(), "%.2f mm", weather.getPrecipitation().getQpfRainMm());
            qpfSnow = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfSnowIn()) :
                    String.format(Locale.getDefault(), "%.2f cm", weather.getPrecipitation().getQpfSnowCm());
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
        chance = qpfRain = qpfSnow = "";
        alerts.clear();
    }
}
