package com.thewizrd.shared_resources.weatherdata.openweather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class AlertsItem {

    @SerializedName("start")
    private long start;

    @SerializedName("description")
    private String description;

    @SerializedName("sender_name")
    private String senderName;

    @SerializedName("end")
    private long end;

    @SerializedName("event")
    private String event;

    public void setStart(long start) {
        this.start = start;
    }

    public long getStart() {
        return start;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getEnd() {
        return end;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }
}