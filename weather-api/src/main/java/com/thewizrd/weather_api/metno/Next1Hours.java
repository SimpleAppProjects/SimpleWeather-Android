package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Next1Hours {

    @Json(name = "summary")
    private Summary summary;

    @Json(name = "details")
    private Details2 details;

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setDetails(Details2 details) {
        this.details = details;
    }

    public Details2 getDetails() {
        return details;
    }
}