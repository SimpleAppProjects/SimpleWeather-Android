package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class EventValues {

	/*
	@SerializedName("distance")
	private int distance;

	@SerializedName("geocodeType")
	private String geocodeType;
	 */

    @SerializedName("response")
    private List<ResponseItem> response;

    @SerializedName("origin")
    private String origin;

	/*
	@SerializedName("geocode")
	private String geocode;

	@SerializedName("link")
	private String link;
	 */

    @SerializedName("description")
    private String description;

	/*
	@SerializedName("location")
	private Location location;
	 */

    @SerializedName("title")
    private String title;

    @SerializedName("headline")
    private String headline;

	/*
	@SerializedName("direction")
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