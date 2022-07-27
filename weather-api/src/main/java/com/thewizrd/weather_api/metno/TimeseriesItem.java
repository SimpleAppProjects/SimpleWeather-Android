package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class TimeseriesItem {

    @Json(name = "data")
    private Data data;

    @Json(name = "time")
    private String time;

    public void setData(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }
}