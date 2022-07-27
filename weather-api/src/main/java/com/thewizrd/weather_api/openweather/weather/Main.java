package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Main {

    @Json(name = "temp")
    private float temp;

    @Json(name = "feels_like")
    private Float feelsLike;

    @Json(name = "temp_min")
    private float tempMin;

    @Json(name = "temp_max")
    private float tempMax;

    @Json(name = "humidity")
    private int humidity;

    @Json(name = "pressure")
    private float pressure;

    @Json(name = "sea_level")
    private Float seaLevel;

    @Json(name = "grnd_level")
    private Float grndLevel;

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getTemp() {
        return temp;
    }

    public void setFeelsLike(Float feelsLike) {
        this.feelsLike = feelsLike;
    }

    public Float getFeelsLike() {
        return feelsLike;
    }

    public void setTempMin(float tempMin) {
        this.tempMin = tempMin;
    }

    public float getTempMin() {
        return tempMin;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getPressure() {
        return pressure;
    }

    public void setTempMax(float tempMax) {
        this.tempMax = tempMax;
    }

    public float getTempMax() {
        return tempMax;
    }

    public Float getSeaLevel() {
        return seaLevel;
    }

    public void setSeaLevel(Float seaLevel) {
        this.seaLevel = seaLevel;
    }

    public Float getGrndLevel() {
        return grndLevel;
    }

    public void setGrndLevel(Float grndLevel) {
        this.grndLevel = grndLevel;
    }
}