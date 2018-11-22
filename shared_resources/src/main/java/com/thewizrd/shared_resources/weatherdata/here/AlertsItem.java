package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AlertsItem {

    @SerializedName("timeSegment")
    private List<TimeSegmentItem> timeSegment;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    public void setTimeSegment(List<TimeSegmentItem> timeSegment) {
        this.timeSegment = timeSegment;
    }

    public List<TimeSegmentItem> getTimeSegment() {
        return timeSegment;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}