package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Main {

    @Json(name = "temp")
    private Float temp;

    @Json(name = "feels_like")
    private Float feelsLike;

    @Json(name = "temp_min")
    private Float tempMin;

    @Json(name = "temp_max")
    private Float tempMax;

    @Json(name = "humidity")
    private Integer humidity;

    @Json(name = "pressure")
    private Float pressure;

    @Json(name = "sea_level")
    private Float seaLevel;

    @Json(name = "grnd_level")
    private Float grndLevel;

    public void setTemp(Float temp) {
        this.temp = temp;
    }

    public Float getTemp() {
        return temp;
    }

    public void setFeelsLike(Float feelsLike) {
        this.feelsLike = feelsLike;
    }

    public Float getFeelsLike() {
        return feelsLike;
    }

    public void setTempMin(Float tempMin) {
        this.tempMin = tempMin;
    }

    public Float getTempMin() {
        return tempMin;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setPressure(Float pressure) {
        this.pressure = pressure;
    }

    public Float getPressure() {
        return pressure;
    }

    public void setTempMax(Float tempMax) {
        this.tempMax = tempMax;
    }

    public Float getTempMax() {
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