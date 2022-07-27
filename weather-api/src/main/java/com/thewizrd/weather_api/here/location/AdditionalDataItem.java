package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class AdditionalDataItem {

    @Json(name = "value")
    private String value;

    @Json(name = "key")
    private String key;

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}