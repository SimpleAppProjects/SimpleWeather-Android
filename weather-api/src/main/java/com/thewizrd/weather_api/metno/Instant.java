package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Instant {

    @Json(name = "details")
    private Details details;

    public void setDetails(Details details) {
        this.details = details;
    }

    public Details getDetails() {
        return details;
    }
}