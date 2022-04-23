package com.thewizrd.weather_api.locationiq;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class GeoLocation {

    @SerializedName("osm_type")
    private String osmType;

    @SerializedName("osm_id")
    private String osmId;

    @SerializedName("licence")
    private String licence;

    @SerializedName("boundingbox")
    private List<String> boundingbox;

    @SerializedName("address")
    private Address address;

    @SerializedName("lon")
    private String lon;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("place_id")
    private String placeId;

    @SerializedName("lat")
    private String lat;

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getLicence() {
        return licence;
    }

    public void setBoundingbox(List<String> boundingbox) {
        this.boundingbox = boundingbox;
    }

    public List<String> getBoundingbox() {
        return boundingbox;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLat() {
        return lat;
    }
}