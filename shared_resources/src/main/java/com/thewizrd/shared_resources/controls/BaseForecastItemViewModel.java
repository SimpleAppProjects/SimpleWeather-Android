package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseForecastItemViewModel extends ObservableViewModel {
    protected WeatherManager wm;

    protected String weatherIcon;
    protected String date;
    protected String condition;
    protected String hiTemp;
    protected String pop;
    protected int windDirection;
    protected String windSpeed;
    protected String windDir;

    protected List<DetailItemViewModel> detailExtras;

    public BaseForecastItemViewModel() {
        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>();
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

    public String getWindDirLabel() {
        return windDir;
    }

    public void setWindDirLabel(String windDirection) {
        this.windDir = windDirection;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public List<DetailItemViewModel> getExtras() {
        return detailExtras;
    }

    public void setExtras(List<DetailItemViewModel> extras) {
        this.detailExtras = extras;
    }
}
