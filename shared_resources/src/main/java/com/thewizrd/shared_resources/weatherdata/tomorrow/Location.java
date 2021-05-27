package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Location {

    @SerializedName("coordinates")
    private List<Double> coordinates;

    @SerializedName("type")
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