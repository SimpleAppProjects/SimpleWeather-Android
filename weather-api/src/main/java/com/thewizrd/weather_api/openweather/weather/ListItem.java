package com.thewizrd.weather_api.openweather.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ListItem {

    @SerializedName("dt")
    private long dt;

    @SerializedName("rain")
    private Rain rain;

    @SerializedName("snow")
    private Snow snow;

    @SerializedName("dt_txt")
    private String dtTxt;

    @SerializedName("weather")
    private List<WeatherItem> weather;

    @SerializedName("main")
    private Main main;

    @SerializedName("clouds")
    private Clouds clouds;

    @SerializedName("sys")
    private ForecastSys sys;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("visibility")
    private Integer visibility;

    @SerializedName("pop")
    private Float pop;

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public void setRain(Rain rain) {
        this.rain = rain;
    }

    public Rain getRain() {
        return rain;
    }

    public Snow getSnow() {
        return snow;
    }

    public void setSnow(Snow snow) {
        this.snow = snow;
    }

    public void setDtTxt(String dtTxt) {
        this.dtTxt = dtTxt;
    }

    public String getDtTxt() {
        return dtTxt;
    }

    public void setWeather(List<WeatherItem> weather) {
        this.weather = weather;
    }

    public List<WeatherItem> getWeather() {
        return weather;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public Main getMain() {
        return main;
    }

    public void setClouds(Clouds clouds) {
        this.clouds = clouds;
    }

    public Clouds getClouds() {
        return clouds;
    }

    public void setSys(ForecastSys sys) {
        this.sys = sys;
    }

    public ForecastSys getSys() {
        return sys;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Wind getWind() {
        return wind;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setPop(Float pop) {
        this.pop = pop;
    }

    public Float getPop() {
        return pop;
    }
}