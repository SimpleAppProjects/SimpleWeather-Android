package com.thewizrd.weather_api.nws.hourly;

import com.squareup.moshi.Json;

public class Location {

    @Json(name = "latitude")
    private double latitude;

    @Json(name = "longitude")
    private double longitude;

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }
}