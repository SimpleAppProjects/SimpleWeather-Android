package com.thewizrd.shared_resources.weatherdata.nws.observation;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Data {

    @SerializedName("pop")
    private List<String> pop;

    @SerializedName("iconLink")
    private List<String> iconLink;

    @SerializedName("hazard")
    private List<String> hazard;

    @SerializedName("temperature")
    private List<String> temperature;

    @SerializedName("weather")
    private List<String> weather;

    @SerializedName("text")
    private List<String> text;

    @SerializedName("hazardUrl")
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