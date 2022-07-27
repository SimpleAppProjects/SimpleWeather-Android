package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class AlertsData {

    @Json(name = "events")
    private List<EventsItem> events;

    public void setEvents(List<EventsItem> events) {
        this.events = events;
    }

    public List<EventsItem> getEvents() {
        return events;
    }
}