package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Meta {

    @Json(name = "timesteps")
	private List<String> timesteps;

    @Json(name = "timestep")
	private String timestep;

    @Json(name = "from")
	private String from;

    @Json(name = "to")
	private String to;

    @Json(name = "field")
	private String field;

	public void setTimesteps(List<String> timesteps) {
		this.timesteps = timesteps;
	}

	public List<String> getTimesteps() {
		return timesteps;
	}

	public void setTimestep(String timestep) {
		this.timestep = timestep;
	}

	public String getTimestep() {
		return timestep;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}
}