package com.thewizrd.weather_api.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class IntervalsItem {

	@SerializedName("values")
	private Values values;

	@SerializedName("startTime")
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