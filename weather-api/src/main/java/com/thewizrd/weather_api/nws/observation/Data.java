package com.thewizrd.weather_api.nws.observation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class Data {

    @Json(name = "pop")
    private List<String> pop;

    @Json(name = "iconLink")
    private List<String> iconLink;

    @Json(name = "hazard")
    private List<String> hazard;

    @Json(name = "temperature")
    private List<String> temperature;

    @Json(name = "weather")
    private List<String> weather;

    @Json(name = "text")
    private List<String> text;

    @Json(name = "hazardUrl")
    private List<String> hazardUrl;

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

    public void setHazard(List<String> hazard) {
        this.hazard = hazard;
    }

    public List<String> getHazard() {
        return hazard;
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

    public void setText(List<String> text) {
        this.text = text;
    }

    public List<String> getText() {
        return text;
    }

    public void setHazardUrl(List<String> hazardUrl) {
        this.hazardUrl = hazardUrl;
    }

    public List<String> getHazardUrl() {
        return hazardUrl;
    }
}