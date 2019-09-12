package com.thewizrd.shared_resources.controls;

import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseForecastItemViewModel extends ViewModel {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseForecastItemViewModel that = (BaseForecastItemViewModel) o;

        if (getWindDirection() != that.getWindDirection()) return false;
        if (getWeatherIcon() != null ? !getWeatherIcon().equals(that.getWeatherIcon()) : that.getWeatherIcon() != null)
            return false;
        if (getDate() != null ? !getDate().equals(that.getDate()) : that.getDate() != null)
            return false;
        if (getCondition() != null ? !getCondition().equals(that.getCondition()) : that.getCondition() != null)
            return false;
        if (getHiTemp() != null ? !getHiTemp().equals(that.getHiTemp()) : that.getHiTemp() != null)
            return false;
        if (getPop() != null ? !getPop().equals(that.getPop()) : that.getPop() != null)
            return false;
        if (getWindSpeed() != null ? !getWindSpeed().equals(that.getWindSpeed()) : that.getWindSpeed() != null)
            return false;
        if (windDir != null ? !windDir.equals(that.windDir) : that.windDir != null) return false;
        return detailExtras != null ? detailExtras.equals(that.detailExtras) : that.detailExtras == null;
    }

    @Override
    public int hashCode() {
        int result = getDate() != null ? getDate().hashCode() : 0;
        result = 31 * result + (getCondition() != null ? getCondition().hashCode() : 0);
        result = 31 * result + (getHiTemp() != null ? getHiTemp().hashCode() : 0);
        result = 31 * result + (getPop() != null ? getPop().hashCode() : 0);
        result = 31 * result + getWindDirection();
        result = 31 * result + (getWindSpeed() != null ? getWindSpeed().hashCode() : 0);
        result = 31 * result + (windDir != null ? windDir.hashCode() : 0);
        result = 31 * result + (detailExtras != null ? detailExtras.hashCode() : 0);
        return result;
    }
}
