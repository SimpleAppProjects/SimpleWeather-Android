package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Location {

    @SerializedName("country")
    private String country;

    @SerializedName("city")
    private String city;

    @SerializedName("woeid")
    private String woeid;

    @SerializedName("timezone_id")
    private String timezoneId;

    @SerializedName("region")
    private String region;

    @SerializedName("lat")
    private String lat;

    @SerializedName("long")
    private String _long;

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

    public void setWoeid(String woeid) {
        this.woeid = woeid;
    }

    public String getWoeid() {
        return woeid;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLat() {
        return lat;
    }

    public void set_long(String _long) {
        this._long = _long;
    }

    public String get_long() {
        return _long;
    }
}