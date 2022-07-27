package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class WeatherItem {

    @Json(name = "icon")
	private String icon;

    @Json(name = "description")
	private String description;

    @Json(name = "main")
	private String main;

    @Json(name = "id")
	private int id;

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMain() {
		return main;
	}

	public void setMain(String main) {
		this.main = main;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}