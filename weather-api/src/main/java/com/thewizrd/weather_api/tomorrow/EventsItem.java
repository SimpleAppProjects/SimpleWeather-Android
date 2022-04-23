package com.thewizrd.weather_api.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class EventsItem {

    @SerializedName("severity")
    private String severity;

    @SerializedName("insight")
    private String insight;

    @SerializedName("urgency")
    private String urgency;

    @SerializedName("certainty")
    private String certainty;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("updateTime")
    private String updateTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("eventValues")
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