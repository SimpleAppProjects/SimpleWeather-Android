package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Coord {

    @Json(name = "lon")
    private float lon;

    @Json(name = "lat")
    private float lat;

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getLon() {
        return lon;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLat() {
        return lat;
    }
}