package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class HourlyForecastItem {

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("sky")
    private String sky;

    @SerializedName("wdir")
    private Wdir wdir;

    @SerializedName("wx")
    private String wx;

    @SerializedName("temp")
    private Temp temp;

    @SerializedName("dewpoint")
    private Dewpoint dewpoint;

    @SerializedName("feelslike")
    private Feelslike feelslike;

    @SerializedName("qpf")
    private Qpf qpf;

    @SerializedName("wspd")
    private Wspd wspd;

    @SerializedName("icon")
    private String icon;

    @SerializedName("uvi")
    private String uvi;

    @SerializedName("FCTTIME")
    private FCTTIME fCTTIME;

    @SerializedName("heatindex")
    private Heatindex heatindex;

    @SerializedName("pop")
    private String pop;

    @SerializedName("condition")
    private String condition;

    @SerializedName("snow")
    private Snow snow;

    @SerializedName("fctcode")
    private String fctcode;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("mslp")
    private Mslp mslp;

    @SerializedName("windchill")
    private Windchill windchill;

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setSky(String sky) {
        this.sky = sky;
    }

    public String getSky() {
        return sky;
    }

    public void setWdir(Wdir wdir) {
        this.wdir = wdir;
    }

    public Wdir getWdir() {
        return wdir;
    }

    public void setWx(String wx) {
        this.wx = wx;
    }

    public String getWx() {
        return wx;
    }

    public void setTemp(Temp temp) {
        this.temp = temp;
    }

    public Temp getTemp() {
        return temp;
    }

    public void setDewpoint(Dewpoint dewpoint) {
        this.dewpoint = dewpoint;
    }

    public Dewpoint getDewpoint() {
        return dewpoint;
    }

    public void setFeelslike(Feelslike feelslike) {
        this.feelslike = feelslike;
    }

    public Feelslike getFeelslike() {
        return feelslike;
    }

    public void setQpf(Qpf qpf) {
        this.qpf = qpf;
    }

    public Qpf getQpf() {
        return qpf;
    }

    public void setWspd(Wspd wspd) {
        this.wspd = wspd;
    }

    public Wspd getWspd() {
        return wspd;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setUvi(String uvi) {
        this.uvi = uvi;
    }

    public String getUvi() {
        return uvi;
    }

    public void setFCTTIME(FCTTIME fCTTIME) {
        this.fCTTIME = fCTTIME;
    }

    public FCTTIME getFCTTIME() {
        return fCTTIME;
    }

    public void setHeatindex(Heatindex heatindex) {
        this.heatindex = heatindex;
    }

    public Heatindex getHeatindex() {
        return heatindex;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public String getPop() {
        return pop;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public void setSnow(Snow snow) {
        this.snow = snow;
    }

    public Snow getSnow() {
        return snow;
    }

    public void setFctcode(String fctcode) {
        this.fctcode = fctcode;
    }

    public String getFctcode() {
        return fctcode;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setMslp(Mslp mslp) {
        this.mslp = mslp;
    }

    public Mslp getMslp() {
        return mslp;
    }

    public void setWindchill(Windchill windchill) {
        this.windchill = windchill;
    }

    public Windchill getWindchill() {
        return windchill;
    }
}