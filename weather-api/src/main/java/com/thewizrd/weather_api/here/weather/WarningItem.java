package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class WarningItem {

    @Json(name = "severity")
    private int severity;

    @Json(name = "country")
    private String country;

    @Json(name = "latitude")
    private double latitude;

    @Json(name = "county")
    private List<CountyItem> county;

    @Json(name = "description")
    private String description;

    @Json(name = "type")
    private String type;

    @Json(name = "message")
    private String message;

    @Json(name = "validFromTimeLocal")
    private String validFromTimeLocal;

    @Json(name = "validUntilTimeLocal")
    private String validUntilTimeLocal;

    @Json(name = "name")
    private String name;

    //@Json(name = "location")
    //private List<Object> location;

    @Json(name = "state")
    private String state;

    @Json(name = "longitude")
    private double longitude;

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setCounty(List<CountyItem> county) {
        this.county = county;
    }

    public List<CountyItem> getCounty() {
        return county;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setValidFromTimeLocal(String validFromTimeLocal) {
        this.validFromTimeLocal = validFromTimeLocal;
    }

    public String getValidFromTimeLocal() {
        return validFromTimeLocal;
    }

    public void setValidUntilTimeLocal(String validUntilTimeLocal) {
        this.validUntilTimeLocal = validUntilTimeLocal;
    }

    public String getValidUntilTimeLocal() {
        return validUntilTimeLocal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

	/*
	public void setLocation(List<Object> location){
		this.location = location;
	}

	public List<Object> getLocation(){
		return location;
	}
	 */

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }
}