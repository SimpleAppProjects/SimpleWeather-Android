package com.thewizrd.weather_api.locationiq;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class AutoCompleteQuery {

    @Json(name = "osm_id")
    private String osmId;

    @Json(name = "licence")
    private String licence;

    @Json(name = "boundingbox")
    private List<String> boundingbox;

    @Json(name = "address")
    private Address address;

    @Json(name = "display_address")
    private String displayAddress;

    @Json(name = "lon")
    private String lon;

    @Json(name = "type")
    private String type;

    @Json(name = "display_name")
    private String displayName;

    @Json(name = "display_place")
    private String displayPlace;

    @Json(name = "osm_type")
    private String osmType;

    @Json(name = "class")
    private String jsonMemberClass;

    @Json(name = "place_id")
    private String placeId;

    @Json(name = "lat")
    private String lat;

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

    public void setDisplayAddress(String displayAddress) {
        this.displayAddress = displayAddress;
    }

    public String getDisplayAddress() {
        return displayAddress;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayPlace(String displayPlace) {
        this.displayPlace = displayPlace;
    }

    public String getDisplayPlace() {
        return displayPlace;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setJsonMemberClass(String jsonMemberClass) {
        this.jsonMemberClass = jsonMemberClass;
    }

    public String getJsonMemberClass() {
        return jsonMemberClass;
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