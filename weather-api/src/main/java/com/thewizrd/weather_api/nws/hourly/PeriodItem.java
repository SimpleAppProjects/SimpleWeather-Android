package com.thewizrd.weather_api.nws.hourly;

import androidx.annotation.RestrictTo;

public class PeriodItem {
    private String unixTime;
    private String windChill;
    private String windSpeed;
    private String cloudAmount;
    private String pop;
    private String relativeHumidity;
    private String windGust;
    private String temperature;
    private String windDirection;
    private String iconLink;
    private String weather;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PeriodItem(String unixTime, String windChill, String windSpeed, String cloudAmount,
                      String pop, String relativeHumidity, String windGust, String temperature,
                      String windDirection, String iconLink, String weather) {
        this.unixTime = unixTime;
        this.windChill = windChill;
        this.windSpeed = windSpeed;
        this.cloudAmount = cloudAmount;
        this.pop = pop;
        this.relativeHumidity = relativeHumidity;
        this.windGust = windGust;
        this.temperature = temperature;
        this.windDirection = windDirection;
        this.iconLink = iconLink;
        this.weather = weather;
    }

    public String getUnixTime() {
        return unixTime;
    }

    public void setUnixTime(String unixTime) {
        this.unixTime = unixTime;
    }

    public String getWindChill() {
        return windChill;
    }

    public void setWindChill(String windChill) {
        this.windChill = windChill;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getCloudAmount() {
        return cloudAmount;
    }

    public void setCloudAmount(String cloudAmount) {
        this.cloudAmount = cloudAmount;
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public String getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setRelativeHumidity(String relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public String getWindGust() {
        return windGust;
    }

    public void setWindGust(String windGust) {
        this.windGust = windGust;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public String getIconLink() {
        return iconLink;
    }

    public void setIconLink(String iconLink) {
        this.iconLink = iconLink;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }
}
