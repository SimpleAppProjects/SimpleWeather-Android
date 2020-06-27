package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class PeriodsItem {

    @SerializedName("detailedForecast")
    private String detailedForecast;

    @SerializedName("temperatureTrend")
    private String temperatureTrend;

    @SerializedName("shortForecast")
    private String shortForecast;

    @SerializedName("icon")
    private String icon;

    @SerializedName("number")
    private int number;

    @SerializedName("temperatureUnit")
    private String temperatureUnit;

    @SerializedName("name")
    private String name;

    @SerializedName("temperature")
    private int temperature;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("isDaytime")
    private boolean isDaytime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("windDirection")
    private String windDirection;

    @SerializedName("windSpeed")
    private String windSpeed;

    public void setDetailedForecast(String detailedForecast) {
        this.detailedForecast = detailedForecast;
    }

    public String getDetailedForecast() {
        return detailedForecast;
    }

    public void setTemperatureTrend(String temperatureTrend) {
        this.temperatureTrend = temperatureTrend;
    }

    public String getTemperatureTrend() {
        return temperatureTrend;
    }

    public void setShortForecast(String shortForecast) {
        this.shortForecast = shortForecast;
    }

    public String getShortForecast() {
        return shortForecast;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setIsDaytime(boolean isDaytime) {
        this.isDaytime = isDaytime;
    }

    public boolean getIsDaytime() {
        return isDaytime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWindSpeed() {
        return windSpeed;
    }
}