package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;

public class AC_RESULTS {

    @SerializedName("ll")
    private String ll;

    @SerializedName("c")
    private String C;

    @SerializedName("zmw")
    private String zmw;

    @SerializedName("tz")
    private String tz;

    @SerializedName("name")
    private String name;

    @SerializedName("lon")
    private String lon;

    @SerializedName("type")
    private String type;

    @SerializedName("tzs")
    private String tzs;

    @SerializedName("l")
    private String L;

    @SerializedName("lat")
    private String lat;

    public void setLl(String ll) {
        this.ll = ll;
    }

    public String getLl() {
        return ll;
    }

    public void setC(String C) {
        this.C = C;
    }

    public String getC() {
        return C;
    }

    public void setZmw(String zmw) {
        this.zmw = zmw;
    }

    public String getZmw() {
        return zmw;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getTz() {
        return tz;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setTzs(String tzs) {
        this.tzs = tzs;
    }

    public String getTzs() {
        return tzs;
    }

    public void setL(String L) {
        this.L = L;
    }

    public String getL() {
        return L;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLat() {
        return lat;
    }
}