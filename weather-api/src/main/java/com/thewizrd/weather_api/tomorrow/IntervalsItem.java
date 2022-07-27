package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class IntervalsItem {

    @Json(name = "values")
	private Values values;

    @Json(name = "startTime")
	private String startTime;

	public void setValues(Values values) {
		this.values = values;
	}

	public Values getValues() {
		return values;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getStartTime() {
		return startTime;
	}
}