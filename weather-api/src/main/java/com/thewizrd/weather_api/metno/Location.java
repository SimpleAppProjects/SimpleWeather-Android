package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Location {

    @SerializedName("latitude")
    private String latitude;

    @SerializedName("time")
    private List<TimeItem> time;

    @SerializedName("height")
    private String height;

    @SerializedName("longitude")
    private String longitude;

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setTime(List<TimeItem> time) {
        this.time = time;
    }

    public List<TimeItem> getTime() {
        return time;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }
}