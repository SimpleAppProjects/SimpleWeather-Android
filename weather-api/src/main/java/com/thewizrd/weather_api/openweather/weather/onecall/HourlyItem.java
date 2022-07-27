package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class HourlyItem {

    @Json(name = "dt")
    private long dt;

    @Json(name = "temp")
    private float temp;

    @Json(name = "wind_deg")
    private int windDeg;

    @Json(name = "dew_point")
    private float dewPoint;

    @Json(name = "weather")
    private List<WeatherItem> weather;

    @Json(name = "humidity")
    private int humidity;

    @Json(name = "wind_speed")
    private float windSpeed;

    @Json(name = "wind_gust")
    private Float windGust;

    @Json(name = "pressure")
    private float pressure;

    @Json(name = "clouds")
    private int clouds;

    @Json(name = "pop")
    private Float pop;

    @Json(name = "feels_like")
    private float feelsLike;

    @Json(name = "rain")
    private Rain rain;

    @Json(name = "snow")
    private Snow snow;

    @Json(name = "visibility")
    private Integer visibility;

    @Json(name = "uvi")
    private Float uvi;

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public int getWindDeg() {
        return windDeg;
    }

    public void setWindDeg(int windDeg) {
        this.windDeg = windDeg;
    }

    public float getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(float dewPoint) {
        this.dewPoint = dewPoint;
    }

    public List<WeatherItem> getWeather() {
        return weather;
    }

    public void setWeather(List<WeatherItem> weather) {
        this.weather = weather;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Float getWindGust() {
        return windGust;
    }

    public void setWindGust(Float windGust) {
        this.windGust = windGust;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public int getClouds() {
        return clouds;
    }

    public void setClouds(int clouds) {
        this.clouds = clouds;
    }

    public Float getPop() {
        return pop;
    }

    public void setPop(Float pop) {
        this.pop = pop;
    }

    public float getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(float feelsLike) {
        this.feelsLike = feelsLike;
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

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public void setUvi(Float uvi) {
        this.uvi = uvi;
    }

    public Float getUvi() {
        return uvi;
    }
}