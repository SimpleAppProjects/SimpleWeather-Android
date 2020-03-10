package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class RelativeLocation {

    @SerializedName("distance")
    private Distance distance;

    @SerializedName("city")
    private String city;

    @SerializedName("bearing")
    private Bearing bearing;

    @SerializedName("geometry")
    private String geometry;

    @SerializedName("state")
    private String state;

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setBearing(Bearing bearing) {
        this.bearing = bearing;
    }

    public Bearing getBearing() {
        return bearing;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}