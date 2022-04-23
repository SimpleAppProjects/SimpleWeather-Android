package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Geometry {

    @SerializedName("coordinates")
    private List<Float> coordinates;

    @SerializedName("type")
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