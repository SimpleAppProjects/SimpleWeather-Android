package com.thewizrd.weather_api.nws.observation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class ForecastResponse {

    @Json(name = "operationalMode")
	private String operationalMode;

    @Json(name = "srsName")
	private String srsName;

    @Json(name = "moreInformation")
	private String moreInformation;

    @Json(name = "data")
	private Data data;

    @Json(name = "location")
	private Location location;

    @Json(name = "time")
	private Time time;

    @Json(name = "creationDate")
	private String creationDate;

    @Json(name = "creationDateLocal")
	private String creationDateLocal;

    @Json(name = "credit")
	private String credit;

    @Json(name = "currentobservation")
	private Currentobservation currentobservation;

    @Json(name = "productionCenter")
	private String productionCenter;

	public void setOperationalMode(String operationalMode) {
		this.operationalMode = operationalMode;
	}

	public String getOperationalMode() {
		return operationalMode;
	}

	public void setSrsName(String srsName) {
		this.srsName = srsName;
	}

	public String getSrsName() {
		return srsName;
	}

	public void setMoreInformation(String moreInformation) {
		this.moreInformation = moreInformation;
	}

	public String getMoreInformation() {
		return moreInformation;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public Data getData() {
		return data;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public void setTime(Time time) {
		this.time = time;
	}

	public Time getTime() {
		return time;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDateLocal(String creationDateLocal) {
		this.creationDateLocal = creationDateLocal;
	}

	public String getCreationDateLocal() {
		return creationDateLocal;
	}

	public void setCredit(String credit) {
		this.credit = credit;
	}

	public String getCredit() {
		return credit;
	}

	public void setCurrentobservation(Currentobservation currentobservation) {
		this.currentobservation = currentobservation;
	}

	public Currentobservation getCurrentobservation() {
		return currentobservation;
	}

	public void setProductionCenter(String productionCenter) {
		this.productionCenter = productionCenter;
	}

	public String getProductionCenter() {
		return productionCenter;
	}
}