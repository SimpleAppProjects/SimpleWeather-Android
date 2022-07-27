package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;

import java.util.List;

public class Location {

    @Json(name = "coordinates")
    private List<Double> coordinates;

    @Json(name = "type")
    private String type;

    public void setCoordinates(List<Double> coordinates) {
        this.coordinates = coordinates;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}