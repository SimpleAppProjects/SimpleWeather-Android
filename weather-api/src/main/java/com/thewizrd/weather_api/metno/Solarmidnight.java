package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Solarmidnight {

    @Json(name = "elevation")
    private String elevation;

    @Json(name = "time")
    private String time;

    @Json(name = "desc")
    private String desc;

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getElevation() {
        return elevation;
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