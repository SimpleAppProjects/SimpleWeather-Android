package com.thewizrd.shared_resources.weatherdata.openweather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class DailyItem {

    @SerializedName("sunrise")
    private long sunrise;

    @SerializedName("temp")
    private Temp temp;

    @SerializedName("uvi")
    private float uvi;

    @SerializedName("pressure")
    private float pressure;

    @SerializedName("clouds")
    private int clouds;

    @SerializedName("pop")
    private Float pop;

    @SerializedName("feels_like")
    private FeelsLike feelsLike;

    @SerializedName("dt")
    private long dt;

    @SerializedName("wind_deg")
    private int windDeg;

    @SerializedName("dew_point")
    private float dewPoint;

    @SerializedName("sunset")
    private long sunset;

    @SerializedName("weather")
    private List<WeatherItem> weather;

    @SerializedName("humidity")
    private int humidity;

    @SerializedName("wind_speed")
    private float windSpeed;

    @SerializedName("wind_gust")
    private Float windGust;

    @SerializedName("rain")
    private Float rain;

    @SerializedName("snow")
    private Float snow;

    @SerializedName("visibility")
    private Integer visibility;

    @SerializedName("moonrise")
    private Long moonrise;

    @SerializedName("moonset")
    private Long moonset;

    @SerializedName("moon_phase")
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