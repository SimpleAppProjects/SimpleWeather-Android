package com.thewizrd.shared_resources.weatherdata.nws.observation;

import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

public class PeriodsItem {
    private String name;
    private String startTime;
    private String tempLabel;
    private String temperature;
    private String pop;
    private String shortForecast;
    private String icon;
    private String detailedForecast;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PeriodsItem(String name, String startTime, String tempLabel, String temperature, String pop,
                       String shortForecast, String icon, String txtForecast) {
        this.name = name;
        this.startTime = startTime;
        this.tempLabel = tempLabel;
        this.temperature = temperature;
        this.pop = pop;
        this.shortForecast = shortForecast;
        this.icon = icon;
        this.detailedForecast = txtForecast;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public boolean getIsDaytime() {
        return ObjectsCompat.equals("High", tempLabel);
    }

    public String getTempLabel() {
        return tempLabel;
    }

    public void setTempLabel(String tempLabel) {
        this.tempLabel = tempLabel;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public String getShortForecast() {
        return shortForecast;
    }

    public void setShortForecast(String shortForecast) {
        this.shortForecast = shortForecast;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDetailedForecast() {
        return detailedForecast;
    }

    public void setDetailedForecast(String detailedForecast) {
        this.detailedForecast = detailedForecast;
    }
}
