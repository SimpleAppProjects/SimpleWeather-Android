package com.thewizrd.weather_api.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Data {

	@SerializedName("timelines")
	private List<TimelinesItem> timelines;

	public void setTimelines(List<TimelinesItem> timelines) {
		this.timelines = timelines;
	}

	public List<TimelinesItem> getTimelines() {
		return timelines;
	}
}