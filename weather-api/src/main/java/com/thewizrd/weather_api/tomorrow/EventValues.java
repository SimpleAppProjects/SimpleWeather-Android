package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class EventValues {

	/*
	@Json(name = "distance")
	private int distance;

	@Json(name = "geocodeType")
	private String geocodeType;
	 */

    @Json(name = "response")
    private List<ResponseItem> response;

    @Json(name = "origin")
    private String origin;

	/*
	@Json(name = "geocode")
	private String geocode;

	@Json(name = "link")
	private String link;
	 */

    @Json(name = "description")
    private String description;

	/*
	@Json(name = "location")
	private Location location;
	 */

    @Json(name = "title")
    private String title;

    @Json(name = "headline")
    private String headline;

	/*
	@Json(name = "direction")
	private double direction;

	public void setDistance(int distance){
		this.distance = distance;
	}

	public int getDistance(){
		return distance;
	}

	public void setGeocodeType(String geocodeType){
		this.geocodeType = geocodeType;
	}

	public String getGeocodeType(){
		return geocodeType;
	}
	 */

    public void setResponse(List<ResponseItem> response) {
        this.response = response;
    }

    public List<ResponseItem> getResponse() {
        return response;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

	/*
	public void setGeocode(String geocode){
		this.geocode = geocode;
	}

	public String getGeocode(){
		return geocode;
	}

	public void setLink(String link){
		this.link = link;
	}

	public String getLink(){
		return link;
	}
	 */

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

	/*
	public void setLocation(Location location){
		this.location = location;
	}

	public Location getLocation(){
		return location;
	}
	*/

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getHeadline() {
        return headline;
    }

	/*
	public void setDirection(double direction){
		this.direction = direction;
	}

	public double getDirection(){
		return direction;
	}
	 */
}