package com.thewizrd.weather_api.here.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class WatchItem {

    @SerializedName("severity")
    private int severity;

    @SerializedName("country")
    private String country;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("message")
    private String message;

    @SerializedName("validFromTimeLocal")
    private String validFromTimeLocal;

    @SerializedName("validUntilTimeLocal")
    private String validUntilTimeLocal;

    //@SerializedName("zone")
    //private List<ZoneItem> zone;

    @SerializedName("name")
    private String name;

    //@SerializedName("location")
    //private List<Object> location;

    @SerializedName("state")
    private String state;

    @SerializedName("longitude")
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

	/*
	public void setZone(List<ZoneItem> zone){
		this.zone = zone;
	}

	public List<ZoneItem> getZone(){
		return zone;
	}
	 */

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