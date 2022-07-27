package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Next6Hours {

    @Json(name = "summary")
    private Summary summary;

    @Json(name = "details")
    private Details3 details;

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setDetails(Details3 details) {
        this.details = details;
    }

    public Details3 getDetails() {
        return details;
    }
}