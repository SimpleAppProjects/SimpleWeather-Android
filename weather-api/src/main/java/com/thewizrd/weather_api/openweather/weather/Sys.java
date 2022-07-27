package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Sys {

    @Json(name = "country")
    private String country;

    @Json(name = "sunrise")
    private int sunrise;

    @Json(name = "sunset")
    private int sunset;

    @Json(name = "id")
    private int id;

    @Json(name = "type")
    private int type;

    @Json(name = "message")
    private String message;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setSunrise(int sunrise) {
        this.sunrise = sunrise;
    }

    public int getSunrise() {
        return sunrise;
    }

    public void setSunset(int sunset) {
        this.sunset = sunset;
    }

    public int getSunset() {
        return sunset;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}