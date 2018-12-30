package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class HourlyForecastItemViewModel {
    private WeatherManager wm;

    private String weatherIcon;
    private String date;
    private String condition;
    private String hiTemp;
    private String pop;
    private int windDirection;
    private String windSpeed;

    public HourlyForecastItemViewModel() {
        wm = WeatherManager.getInstance();
    }

    public HourlyForecastItemViewModel(HourlyForecast hrForecast) {
        wm = WeatherManager.getInstance();

        weatherIcon = hrForecast.getIcon();

        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext()))
            date = hrForecast.getDate().format(DateTimeFormatter.ofPattern("EEE HH:00"));
        else
            date = hrForecast.getDate().format(DateTimeFormatter.ofPattern("EEE h a"));

        condition = hrForecast.getCondition();
        try {
            hiTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%d", Math.round(Double.valueOf(hrForecast.getHighF()))) : String.format(Locale.getDefault(), "%d", Math.round(Double.valueOf(hrForecast.getHighC())))) + "º ";
        } catch (NumberFormatException nFe) {
            hiTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }
        pop = hrForecast.getPop() + "%";
        updateWindDirection(hrForecast.getWindDegrees());
        try {
            windSpeed = (Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%d mph", Math.round(Double.valueOf(hrForecast.getWindMph()))) : String.format(Locale.getDefault(), "%d kph", Math.round(Double.valueOf(hrForecast.getWindKph()))));
        } catch (NumberFormatException nFe) {
            windSpeed = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }
    }

    private void updateWindDirection(int angle) {
        windDirection = angle - 180;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getHiTemp() {
        return hiTemp;
    }

    public void setHiTemp(String hiTemp) {
        this.hiTemp = hiTemp;
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(int windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }
}
