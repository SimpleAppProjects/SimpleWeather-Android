package com.thewizrd.simpleweather.radar.rainviewer;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class RadarItem {

    @Json(name = "path")
    private String path;

    @Json(name = "time")
    private int time;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }
}