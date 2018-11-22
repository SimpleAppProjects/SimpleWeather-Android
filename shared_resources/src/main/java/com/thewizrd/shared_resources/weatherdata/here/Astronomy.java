package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Astronomy {

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

    @SerializedName("astronomy")
    private List<AstronomyItem> astronomy;

    @SerializedName("longitude")
    private float longitude;

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

    public void setAstronomy(List<AstronomyItem> astronomy) {
        this.astronomy = astronomy;
    }

    public List<AstronomyItem> getAstronomy() {
        return astronomy;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
    }
}