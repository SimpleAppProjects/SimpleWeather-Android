package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class TimelinesItem {

    @SerializedName("intervals")
    private List<IntervalsItem> intervals;

    @SerializedName("timestep")
    private String timestep;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
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