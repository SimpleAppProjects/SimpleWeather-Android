package com.thewizrd.weather_api.nws.observation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Location {

    @Json(name = "elevation")
    private String elevation;

    @Json(name = "wfo")
    private String wfo;

    @Json(name = "radar")
    private String radar;

    @Json(name = "metar")
    private String metar;

    @Json(name = "zone")
    private String zone;

    @Json(name = "firezone")
    private String firezone;

    @Json(name = "timezone")
    private String timezone;

    @Json(name = "latitude")
    private String latitude;

    @Json(name = "areaDescription")
    private String areaDescription;

    @Json(name = "county")
    private String county;

    @Json(name = "region")
    private String region;

    @Json(name = "longitude")
    private String longitude;

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getElevation() {
        return elevation;
    }

    public void setWfo(String wfo) {
        this.wfo = wfo;
    }

    public String getWfo() {
        return wfo;
    }

    public void setRadar(String radar) {
        this.radar = radar;
    }

    public String getRadar() {
        return radar;
    }

    public void setMetar(String metar) {
        this.metar = metar;
    }

    public String getMetar() {
        return metar;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getZone() {
        return zone;
    }

    public void setFirezone(String firezone) {
        this.firezone = firezone;
    }

    public String getFirezone() {
        return firezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setAreaDescription(String areaDescription) {
        this.areaDescription = areaDescription;
    }

    public String getAreaDescription() {
        return areaDescription;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCounty() {
        return county;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }
}