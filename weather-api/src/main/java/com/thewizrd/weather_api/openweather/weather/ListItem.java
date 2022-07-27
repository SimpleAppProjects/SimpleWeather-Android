package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class ListItem {

    @Json(name = "dt")
    private long dt;

    @Json(name = "rain")
    private Rain rain;

    @Json(name = "snow")
    private Snow snow;

    @Json(name = "dt_txt")
    private String dtTxt;

    @Json(name = "weather")
    private List<WeatherItem> weather;

    @Json(name = "main")
    private Main main;

    @Json(name = "clouds")
    private Clouds clouds;

    @Json(name = "sys")
    private ForecastSys sys;

    @Json(name = "wind")
    private Wind wind;

    @Json(name = "visibility")
    private Integer visibility;

    @Json(name = "pop")
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