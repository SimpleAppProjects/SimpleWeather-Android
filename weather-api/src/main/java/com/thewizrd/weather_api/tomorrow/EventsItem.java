package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class EventsItem {

    @Json(name = "severity")
    private String severity;

    @Json(name = "insight")
    private String insight;

    @Json(name = "urgency")
    private String urgency;

    @Json(name = "certainty")
    private String certainty;

    @Json(name = "startTime")
    private String startTime;

    @Json(name = "updateTime")
    private String updateTime;

    @Json(name = "endTime")
    private String endTime;

    @Json(name = "eventValues")
    private EventValues eventValues;

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity() {
        return severity;
    }

    public void setInsight(String insight) {
        this.insight = insight;
    }

    public String getInsight() {
        return insight;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setCertainty(String certainty) {
        this.certainty = certainty;
    }

    public String getCertainty() {
        return certainty;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEventValues(EventValues eventValues) {
        this.eventValues = eventValues;
    }

    public EventValues getEventValues() {
        return eventValues;
    }
}