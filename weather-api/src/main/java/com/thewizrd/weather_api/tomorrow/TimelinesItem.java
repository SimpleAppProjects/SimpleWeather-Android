package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class TimelinesItem {

    @Json(name = "intervals")
    private List<IntervalsItem> intervals;

    @Json(name = "timestep")
    private String timestep;

    @Json(name = "startTime")
    private String startTime;

    @Json(name = "endTime")
    private String endTime;

    public void setIntervals(List<IntervalsItem> intervals) {
        this.intervals = intervals;
    }

    public List<IntervalsItem> getIntervals() {
        return intervals;
    }

    public void setTimestep(String timestep) {
        this.timestep = timestep;
    }

    public String getTimestep() {
        return timestep;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEndTime() {
        return endTime;
    }
}