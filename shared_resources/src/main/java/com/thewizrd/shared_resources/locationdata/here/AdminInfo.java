package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;

public class AdminInfo {

    @SerializedName("timeZone")
    private TimeZone timeZone;

    @SerializedName("localTime")
    private String localTime;

    @SerializedName("systemOfMeasure")
    private String systemOfMeasure;

    @SerializedName("currency")
    private String currency;

    @SerializedName("drivingSide")
    private String drivingSide;

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setLocalTime(String localTime) {
        this.localTime = localTime;
    }

    public String getLocalTime() {
        return localTime;
    }

    public void setSystemOfMeasure(String systemOfMeasure) {
        this.systemOfMeasure = systemOfMeasure;
    }

    public String getSystemOfMeasure() {
        return systemOfMeasure;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setDrivingSide(String drivingSide) {
        this.drivingSide = drivingSide;
    }

    public String getDrivingSide() {
        return drivingSide;
    }
}