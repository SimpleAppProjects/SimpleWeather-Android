package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class MetaInfo {

    @Json(name = "nextPageInformation")
    private String nextPageInformation;

    @Json(name = "timestamp")
    private String timestamp;

    public void setNextPageInformation(String nextPageInformation) {
        this.nextPageInformation = nextPageInformation;
    }

    public String getNextPageInformation() {
        return nextPageInformation;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }
}