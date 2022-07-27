package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Location {

    @Json(name = "latitude")
    private String latitude;

    @Json(name = "time")
    private List<TimeItem> time;

    @Json(name = "height")
    private String height;

    @Json(name = "longitude")
    private String longitude;

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setTime(List<TimeItem> time) {
        this.time = time;
    }

    public List<TimeItem> getTime() {
        return time;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }
}