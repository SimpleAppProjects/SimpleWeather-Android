package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class Observations {

    @Json(name = "location")
    private List<LocationItem> location;

    public void setLocation(List<LocationItem> location) {
        this.location = location;
    }

    public List<LocationItem> getLocation() {
        return location;
    }
}