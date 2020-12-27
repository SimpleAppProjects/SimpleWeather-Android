package com.thewizrd.shared_resources.weatherdata.nws.observation;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ForecastResponse {

	@SerializedName("operationalMode")
	private String operationalMode;

	@SerializedName("srsName")
	private String srsName;

	@SerializedName("moreInformation")
	private String moreInformation;

	@SerializedName("data")
	private Data data;

	@SerializedName("location")
	private Location location;

	@SerializedName("time")
	private Time time;

	@SerializedName("creationDate")
	private String creationDate;

	@SerializedName("creationDateLocal")
	private String creationDateLocal;

	@SerializedName("credit")
	private String credit;

	@SerializedName("currentobservation")
	private Currentobservation currentobservation;

	@SerializedName("productionCenter")
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