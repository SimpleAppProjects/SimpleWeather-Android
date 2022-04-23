package com.thewizrd.weather_api.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class AlertsData {

    @SerializedName("events")
    private List<EventsItem> events;

    public void setEvents(List<EventsItem> events) {
        this.events = events;
    }

    public List<EventsItem> getEvents() {
        return events;
    }
}