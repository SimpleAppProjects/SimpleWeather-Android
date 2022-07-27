package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class AdminInfo {

    @Json(name = "timeZone")
    private TimeZone timeZone;

    @Json(name = "localTime")
    private String localTime;

    @Json(name = "systemOfMeasure")
    private String systemOfMeasure;

    @Json(name = "currency")
    private String currency;

    @Json(name = "drivingSide")
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