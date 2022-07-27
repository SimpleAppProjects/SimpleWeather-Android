package com.thewizrd.simpleweather.radar.rainviewer;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Radar {

    @Json(name = "past")
    private List<RadarItem> past;

    @Json(name = "nowcast")
    private List<RadarItem> nowcast;

    public void setPast(List<RadarItem> past) {
        this.past = past;
    }

    public List<RadarItem> getPast() {
        return past;
    }

    public void setNowcast(List<RadarItem> nowcast) {
        this.nowcast = nowcast;
    }

    public List<RadarItem> getNowcast() {
        return nowcast;
    }
}