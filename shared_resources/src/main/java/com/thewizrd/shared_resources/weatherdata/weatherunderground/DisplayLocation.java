package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class DisplayLocation {

    @SerializedName("zip")
    private String zip;

    @SerializedName("magic")
    private String magic;

    @SerializedName("elevation")
    private String elevation;

    @SerializedName("country")
    private String country;

    @SerializedName("country_iso3166")
    private String countryIso3166;

    @SerializedName("city")
    private String city;

    @SerializedName("state_name")
    private String stateName;

    @SerializedName("latitude")
    private String latitude;

    @SerializedName("wmo")
    private String wmo;

    @SerializedName("state")
    private String state;

    @SerializedName("full")
    private String full;

    @SerializedName("longitude")
    private String longitude;

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getZip() {
        return zip;
    }

    public void setMagic(String magic) {
        this.magic = magic;
    }

    public String getMagic() {
        return magic;
    }

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

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getStateName() {
        return stateName;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setWmo(String wmo) {
        this.wmo = wmo;
    }

    public String getWmo() {
        return wmo;
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