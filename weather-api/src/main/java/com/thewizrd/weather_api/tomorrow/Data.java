package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Data {

    @Json(name = "timelines")
	private List<TimelinesItem> timelines;

	public void setTimelines(List<TimelinesItem> timelines) {
		this.timelines = timelines;
	}

	public List<TimelinesItem> getTimelines() {
		return timelines;
	}
}