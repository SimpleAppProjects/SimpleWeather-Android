package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class AlertsItem {

    @Json(name = "start")
    private long start;

    @Json(name = "description")
    private String description;

    @Json(name = "sender_name")
    private String senderName;

    @Json(name = "end")
    private long end;

    @Json(name = "event")
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