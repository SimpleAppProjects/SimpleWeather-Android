package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Coord {

    @Json(name = "lon")
    private Float lon;

    @Json(name = "lat")
    private Float lat;

    public void setLon(Float lon) {
        this.lon = lon;
    }

    public Float getLon() {
        return lon;
    }

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLat() {
        return lat;
    }
}