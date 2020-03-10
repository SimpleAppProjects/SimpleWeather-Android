package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastLocation {

    @SerializedName("country")
    private String country;

    @SerializedName("distance")
    private float distance;

    @SerializedName("city")
    private String city;

    @SerializedName("timezone")
    private int timezone;

    @SerializedName("latitude")
    private float latitude;

    @SerializedName("forecast")
    private List<ForecastItem> forecast;

    @SerializedName("state")
    private String state;

    @SerializedName("longitude")
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

    public void setForecast(List<ForecastItem> forecast) {
        this.forecast = forecast;
    }

    public List<ForecastItem> getForecast() {
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