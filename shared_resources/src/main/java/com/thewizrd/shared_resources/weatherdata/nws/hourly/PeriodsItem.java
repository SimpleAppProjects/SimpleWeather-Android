package com.thewizrd.shared_resources.weatherdata.nws.hourly;

import java.util.List;

public class PeriodsItem {

    private List<String> time;

    private List<String> unixtime;

    private List<String> windChill;

    private List<String> windGust;

    private String periodName;

    private List<String> pop;

    private List<String> iconLink;

    private List<String> relativeHumidity;

    private List<String> temperature;

    private List<String> weather;

    private List<String> windDirection;

    private List<String> windSpeed;

    private List<String> cloudAmount;

    public void setTime(List<String> time) {
        this.time = time;
    }

    public List<String> getTime() {
        return time;
    }

    public void setUnixtime(List<String> unixtime) {
        this.unixtime = unixtime;
    }

    public List<String> getUnixtime() {
        return unixtime;
    }

    public void setWindChill(List<String> windChill) {
        this.windChill = windChill;
    }

    public List<String> getWindChill() {
        return windChill;
    }

    public void setWindGust(List<String> windGust) {
        this.windGust = windGust;
    }

    public List<String> getWindGust() {
        return windGust;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPop(List<String> pop) {
        this.pop = pop;
    }

    public List<String> getPop() {
        return pop;
    }

    public void setIconLink(List<String> iconLink) {
        this.iconLink = iconLink;
    }

    public List<String> getIconLink() {
        return iconLink;
    }

    public void setRelativeHumidity(List<String> relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public List<String> getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setTemperature(List<String> temperature) {
        this.temperature = temperature;
    }

    public List<String> getTemperature() {
        return temperature;
    }

    public void setWeather(List<String> weather) {
        this.weather = weather;
    }

    public List<String> getWeather() {
        return weather;
    }

    public void setWindDirection(List<String> windDirection) {
        this.windDirection = windDirection;
    }

    public List<String> getWindDirection() {
        return windDirection;
    }

    public void setWindSpeed(List<String> windSpeed) {
        this.windSpeed = windSpeed;
    }

    public List<String> getWindSpeed() {
        return windSpeed;
    }

    public void setCloudAmount(List<String> cloudAmount) {
        this.cloudAmount = cloudAmount;
    }

    public List<String> getCloudAmount() {
        return cloudAmount;
    }
}