package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class DailyItem {

    @Json(name = "sunrise")
    private long sunrise;

    @Json(name = "temp")
    private Temp temp;

    @Json(name = "uvi")
    private float uvi;

    @Json(name = "pressure")
    private float pressure;

    @Json(name = "clouds")
    private int clouds;

    @Json(name = "pop")
    private Float pop;

    @Json(name = "feels_like")
    private FeelsLike feelsLike;

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
    private Float rain;

    @Json(name = "snow")
    private Float snow;

    @Json(name = "visibility")
    private Integer visibility;

    @Json(name = "moonrise")
    private Long moonrise;

    @Json(name = "moonset")
    private Long moonset;

    @Json(name = "moon_phase")
    private Float moon_phase;

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public Temp getTemp() {
        return temp;
    }

    public void setTemp(Temp temp) {
        this.temp = temp;
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

    public Float getPop() {
        return pop;
    }

    public void setPop(Float pop) {
        this.pop = pop;
    }

    public FeelsLike getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(FeelsLike feelsLike) {
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

    public Float getRain() {
        return rain;
    }

    public void setRain(Float rain) {
        this.rain = rain;
    }

    public Float getSnow() {
        return snow;
    }

    public void setSnow(Float snow) {
        this.snow = snow;
    }

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Long getMoonrise() {
        return moonrise;
    }

    public void setMoonrise(Long moonrise) {
        this.moonrise = moonrise;
    }

    public Long getMoonset() {
        return moonset;
    }

    public void setMoonset(Long moonset) {
        this.moonset = moonset;
    }

    public Float getMoon_phase() {
        return moon_phase;
    }

    public void setMoon_phase(Float moon_phase) {
        this.moon_phase = moon_phase;
    }
}