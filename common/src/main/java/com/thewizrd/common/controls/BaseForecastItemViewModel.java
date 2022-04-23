package com.thewizrd.common.controls;

import com.thewizrd.shared_resources.ApplicationLibKt;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.weather_api.WeatherModuleKt;
import com.thewizrd.weather_api.weatherdata.WeatherProviderManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class BaseForecastItemViewModel {
    protected final WeatherProviderManager wm;
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

    protected Map<WeatherDetailsType, DetailItemViewModel> detailExtras;

    public BaseForecastItemViewModel() {
        wm = WeatherModuleKt.getWeatherModule().getWeatherManager();
        wim = SharedModuleKt.getSharedDeps().getWeatherIconsManager();
        settingsMgr = ApplicationLibKt.getAppLib().getSettingsManager();
        detailExtras = new LinkedHashMap<>(WeatherDetailsType.values().length);
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

    public Map<WeatherDetailsType, DetailItemViewModel> getExtras() {
        return detailExtras;
    }

    public void setExtras(Map<WeatherDetailsType, DetailItemViewModel> extras) {
        this.detailExtras = extras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseForecastItemViewModel that = (BaseForecastItemViewModel) o;

        if (windDirection != that.windDirection) return false;
        if (!Objects.equals(weatherIcon, that.weatherIcon))
            return false;
        if (!Objects.equals(date, that.date)) return false;
        if (!Objects.equals(shortDate, that.shortDate))
            return false;
        if (!Objects.equals(longDate, that.longDate))
            return false;
        if (!Objects.equals(condition, that.condition))
            return false;
        if (!Objects.equals(hiTemp, that.hiTemp)) return false;
        if (!Objects.equals(windSpeed, that.windSpeed))
            return false;
        if (!Objects.equals(windDir, that.windDir)) return false;
        return Objects.equals(detailExtras, that.detailExtras);
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
