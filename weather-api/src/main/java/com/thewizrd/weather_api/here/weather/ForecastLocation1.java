package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class ForecastLocation1 {

    @Json(name = "country")
    private String country;

    @Json(name = "distance")
    private float distance;

    @Json(name = "city")
    private String city;

    @Json(name = "timezone")
    private int timezone;

    @Json(name = "latitude")
    private float latitude;

    @Json(name = "forecast")
    private List<ForecastItem1> forecast;

    @Json(name = "state")
    private String state;

    @Json(name = "longitude")
    private float longitude;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return distance;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }

    public int getTimezone() {
        return timezone;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setForecast(List<ForecastItem1> forecast) {
        this.forecast = forecast;
    }

    public List<ForecastItem1> getForecast() {
        return forecast;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
    }
}