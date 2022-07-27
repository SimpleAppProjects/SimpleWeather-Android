package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Moonposition {

    @Json(name = "phase")
    private String phase;

    @Json(name = "elevation")
    private String elevation;

    @Json(name = "range")
    private String range;

    @Json(name = "azimuth")
    private String azimuth;

    @Json(name = "time")
    private String time;

    @Json(name = "desc")
    private String desc;

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getElevation() {
        return elevation;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getRange() {
        return range;
    }

    public void setAzimuth(String azimuth) {
        this.azimuth = azimuth;
    }

    public String getAzimuth() {
        return azimuth;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}