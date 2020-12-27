package com.thewizrd.shared_resources.weatherdata.nws.observation;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Location {

    @SerializedName("elevation")
    private String elevation;

    @SerializedName("wfo")
    private String wfo;

    @SerializedName("radar")
    private String radar;

    @SerializedName("metar")
    private String metar;

    @SerializedName("zone")
    private String zone;

    @SerializedName("firezone")
    private String firezone;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("latitude")
    private String latitude;

    @SerializedName("areaDescription")
    private String areaDescription;

    @SerializedName("county")
    private String county;

    @SerializedName("region")
    private String region;

    @SerializedName("longitude")
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