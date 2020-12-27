package com.thewizrd.shared_resources.weatherdata.nws.hourly;

import com.google.gson.annotations.SerializedName;

public class Location {

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
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