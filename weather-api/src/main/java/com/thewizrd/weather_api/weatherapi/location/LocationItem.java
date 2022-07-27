package com.thewizrd.weather_api.weatherapi.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class LocationItem {

    @Json(name = "country")
    private String country;

    @Json(name = "name")
    private String name;

    @Json(name = "lon")
    private double lon;

    @Json(name = "id")
    private int id;

    @Json(name = "region")
    private String region;

    @Json(name = "lat")
    private double lat;

    @Json(name = "url")
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