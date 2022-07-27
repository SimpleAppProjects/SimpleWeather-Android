package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Properties {

    @Json(name = "timeseries")
    private List<TimeseriesItem> timeseries;

    @Json(name = "meta")
    private Meta meta;

    public void setTimeseries(List<TimeseriesItem> timeseries) {
        this.timeseries = timeseries;
    }

    public List<TimeseriesItem> getTimeseries() {
        return timeseries;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Meta getMeta() {
        return meta;
    }
}