package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class City {

	@SerializedName("geo")
	private List<Double> geo;

	@SerializedName("name")
	private String name;

	@SerializedName("url")
	private String url;

	public void setGeo(List<Double> geo) {
		this.geo = geo;
	}

	public List<Double> getGeo() {
		return geo;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}