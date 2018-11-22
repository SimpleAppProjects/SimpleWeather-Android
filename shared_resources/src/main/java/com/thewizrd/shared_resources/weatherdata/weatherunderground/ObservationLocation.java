package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class ObservationLocation {

    @SerializedName("elevation")
    private String elevation;

    @SerializedName("country")
    private String country;

    @SerializedName("country_iso3166")
    private String countryIso3166;

    @SerializedName("city")
    private String city;

    @SerializedName("latitude")
    private String latitude;

    @SerializedName("state")
    private String state;

    @SerializedName("full")
    private String full;

    @SerializedName("longitude")
    private String longitude;

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getElevation() {
        return elevation;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setCountryIso3166(String countryIso3166) {
        this.countryIso3166 = countryIso3166;
    }

    public String getCountryIso3166() {
        return countryIso3166;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setFull(String full) {
        this.full = full;
    }

    public String getFull() {
        return full;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }
}