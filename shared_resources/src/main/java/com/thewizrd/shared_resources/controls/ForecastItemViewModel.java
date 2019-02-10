package com.thewizrd.shared_resources.controls;

import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class ForecastItemViewModel {
    private WeatherManager wm;

    private String weatherIcon;
    private String date;
    private String condition;
    private String hiTemp;
    private String loTemp;

    public ForecastItemViewModel() {
        wm = WeatherManager.getInstance();
    }

    public ForecastItemViewModel(Forecast forecast) {
        wm = WeatherManager.getInstance();

        weatherIcon = forecast.getIcon();
        date = forecast.getDate().format(DateTimeFormatter.ofPattern("EEE dd", Locale.getDefault()));
        condition = forecast.getCondition();
        try {
            hiTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getHighF()))) : String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getHighC())))) + "º ";
        } catch (NumberFormatException nFe) {
            hiTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }
        try {
            loTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getLowF()))) : String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getLowC())))) + "º ";
        } catch (NumberFormatException nFe) {
            loTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }
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

    public String getLoTemp() {
        return loTemp;
    }

    public void setLoTemp(String loTemp) {
        this.loTemp = loTemp;
    }
}
