package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class AlertsRootobject {

    @Json(name = "data")
    private AlertsData data;

    public void setData(AlertsData data) {
        this.data = data;
    }

    public AlertsData getData() {
        return data;
    }
}