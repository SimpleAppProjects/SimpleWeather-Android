package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class GraphItem {

    @SerializedName("elevation")
    private Elevation elevation;

    @SerializedName("stationIdentifier")
    private String stationIdentifier;

    @SerializedName("@type")
    private String type;

    @SerializedName("name")
    private String name;

    @SerializedName("timeZone")
    private String timeZone;

    @SerializedName("geometry")
    private String geometry;

    @SerializedName("@id")
    private String id;

    public void setElevation(Elevation elevation) {
        this.elevation = elevation;
    }

    public Elevation getElevation() {
        return elevation;
    }

    public void setStationIdentifier(String stationIdentifier) {
        this.stationIdentifier = stationIdentifier;
    }

    public String getStationIdentifier() {
        return stationIdentifier;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}