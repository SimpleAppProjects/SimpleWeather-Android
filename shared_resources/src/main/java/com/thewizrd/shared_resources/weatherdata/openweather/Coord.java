package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;

public class Coord {

    @SerializedName("lon")
    private float lon;

    @SerializedName("lat")
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