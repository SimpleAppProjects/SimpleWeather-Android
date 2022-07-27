package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class AlertsItem {

    @Json(name = "timeSegment")
    private List<TimeSegmentItem> timeSegment;

    @Json(name = "description")
    private String description;

    @Json(name = "type")
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