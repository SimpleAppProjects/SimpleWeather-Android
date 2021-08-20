package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseForecastItemViewModel {
    protected final WeatherManager wm;
    protected final WeatherIconsManager wim;
    protected final SettingsManager settingsMgr;

    protected String weatherIcon;
    protected String date;
    protected String shortDate;
    protected String longDate;
    protected String condition;
    protected String hiTemp;
    protected int windDirection;
    protected String windSpeed;
    protected String windDir;

    protected List<DetailItemViewModel> detailExtras;

    public BaseForecastItemViewModel() {
        wm = WeatherManager.getInstance();
        wim = WeatherIconsManager.getInstance();
        settingsMgr = SimpleLibrary.getInstance().getApp().getSettingsManager();

        int capacity = WeatherDetailsType.values().length;
        detailExtras = new ArrayList<>(capacity);
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

    public String getShortDate() {
        return shortDate;
    }

    public void setShortDate(String date) {
        this.shortDate = date;
    }

    public String getLongDate() {
        return longDate;
    }

    public void setLongDate(String date) {
        this.longDate = date;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseForecastItemViewModel that = (BaseForecastItemViewModel) o;

        if (windDirection != that.windDirection) return false;
        if (weatherIcon != null ? !weatherIcon.equals(that.weatherIcon) : that.weatherIcon != null)
            return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (shortDate != null ? !shortDate.equals(that.shortDate) : that.shortDate != null)
            return false;
        if (longDate != null ? !longDate.equals(that.longDate) : that.longDate != null)
            return false;
        if (condition != null ? !condition.equals(that.condition) : that.condition != null)
            return false;
        if (hiTemp != null ? !hiTemp.equals(that.hiTemp) : that.hiTemp != null) return false;
        if (windSpeed != null ? !windSpeed.equals(that.windSpeed) : that.windSpeed != null)
            return false;
        if (windDir != null ? !windDir.equals(that.windDir) : that.windDir != null) return false;
        return detailExtras != null ? detailExtras.equals(that.detailExtras) : that.detailExtras == null;
    }

    @Override
    public int hashCode() {
        int result = weatherIcon != null ? weatherIcon.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (shortDate != null ? shortDate.hashCode() : 0);
        result = 31 * result + (longDate != null ? longDate.hashCode() : 0);
        result = 31 * result + (condition != null ? condition.hashCode() : 0);
        result = 31 * result + (hiTemp != null ? hiTemp.hashCode() : 0);
        result = 31 * result + windDirection;
        result = 31 * result + (windSpeed != null ? windSpeed.hashCode() : 0);
        result = 31 * result + (windDir != null ? windDir.hashCode() : 0);
        result = 31 * result + (detailExtras != null ? detailExtras.hashCode() : 0);
        return result;
    }
}
