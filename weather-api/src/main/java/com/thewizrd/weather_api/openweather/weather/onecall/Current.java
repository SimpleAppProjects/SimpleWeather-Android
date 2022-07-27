package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Current {

    @Json(name = "sunrise")
    private long sunrise;

    @Json(name = "temp")
    private float temp;

    @Json(name = "visibility")
    private int visibility;

    @Json(name = "uvi")
    private float uvi;

    @Json(name = "pressure")
    private float pressure;

    @Json(name = "clouds")
    private int clouds;

    @Json(name = "feels_like")
    private float feelsLike;

    @Json(name = "dt")
    private long dt;

    @Json(name = "wind_deg")
    private int windDeg;

    @Json(name = "dew_point")
    private float dewPoint;

    @Json(name = "sunset")
    private long sunset;

    @Json(name = "weather")
    private List<WeatherItem> weather;

    @Json(name = "humidity")
    private int humidity;

    @Json(name = "wind_speed")
    private float windSpeed;

    @Json(name = "wind_gust")
    private Float windGust;

    @Json(name = "rain")
    private Rain rain;

    @Json(name = "snow")
    private Snow snow;

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public float getUvi() {
        return uvi;
    }

    public void setUvi(float uvi) {
        this.uvi = uvi;
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

    public float getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(float feelsLike) {
        this.feelsLike = feelsLike;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
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

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
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