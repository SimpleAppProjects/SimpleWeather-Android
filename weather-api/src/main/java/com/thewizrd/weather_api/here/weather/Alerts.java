package com.thewizrd.weather_api.here.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Alerts {

    @SerializedName("alerts")
    private List<AlertsItem> alerts;

    @SerializedName("country")
    private String country;

    @SerializedName("city")
    private String city;

    @SerializedName("timezone")
    private int timezone;

    @SerializedName("latitude")
    private float latitude;

    @SerializedName("state")
    private String state;

    @SerializedName("longitude")
    private float longitude;

    public void setAlerts(List<AlertsItem> alerts) {
        this.alerts = alerts;
    }

    public List<AlertsItem> getAlerts() {
        return alerts;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
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