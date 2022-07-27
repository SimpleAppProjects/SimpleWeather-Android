package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class Geometry {

    @Json(name = "coordinates")
    private List<Float> coordinates;

    @Json(name = "type")
    private String type;

    public void setCoordinates(List<Float> coordinates) {
        this.coordinates = coordinates;
    }

    public List<Float> getCoordinates() {
        return coordinates;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}