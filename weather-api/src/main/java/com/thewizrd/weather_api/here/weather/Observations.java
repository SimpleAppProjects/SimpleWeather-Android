package com.thewizrd.weather_api.here.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Observations {

    @SerializedName("location")
    private List<LocationItem> location;

    public void setLocation(List<LocationItem> location) {
        this.location = location;
    }

    public List<LocationItem> getLocation() {
        return location;
    }
}