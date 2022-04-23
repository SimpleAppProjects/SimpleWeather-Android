package com.thewizrd.weather_api.weatherapi.location;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class LocationItem {

    @SerializedName("country")
    private String country;

    @SerializedName("name")
    private String name;

    @SerializedName("lon")
    private double lon;

    @SerializedName("id")
    private int id;

    @SerializedName("region")
    private String region;

    @SerializedName("lat")
    private double lat;

    @SerializedName("url")
    private String url;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLon() {
        return lon;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}