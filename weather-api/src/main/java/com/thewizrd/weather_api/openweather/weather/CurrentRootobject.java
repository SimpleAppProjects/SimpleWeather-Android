package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class CurrentRootobject {

    @Json(name = "dt")
    private long dt;

    @Json(name = "coord")
    private Coord coord;

    @Json(name = "visibility")
    private int visibility;

    @Json(name = "weather")
    private List<WeatherItem> weather;

    @Json(name = "name")
    private String name;

    @Json(name = "cod")
    private int cod;

    @Json(name = "main")
    private Main main;

    @Json(name = "clouds")
    private Clouds clouds;

    @Json(name = "id")
    private int id;

    @Json(name = "sys")
    private CurrentSys sys;

    @Json(name = "timezone")
    private int timezone;

    @Json(name = "base")
    private String base;

    @Json(name = "wind")
    private Wind wind;

    @Json(name = "rain")
    private Rain rain;

    @Json(name = "snow")
    private Snow snow;

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Coord getCoord() {
        return coord;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setWeather(List<WeatherItem> weather) {
        this.weather = weather;
    }

    public List<WeatherItem> getWeather() {
        return weather;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCod(int cod) {
        this.cod = cod;
    }

    public int getCod() {
        return cod;
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

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setSys(CurrentSys sys) {
        this.sys = sys;
    }

    public CurrentSys getSys() {
        return sys;
    }

    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }

    public int getTimezone() {
        return timezone;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getBase() {
        return base;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Wind getWind() {
        return wind;
    }

    public Rain getRain() {
        return rain;
    }

    public void setRain(Rain rain) {
        this.rain = rain;
    }

    public Snow getSnow() {
        return snow;
    }

    public void setSnow(Snow snow) {
        this.snow = snow;
    }
}