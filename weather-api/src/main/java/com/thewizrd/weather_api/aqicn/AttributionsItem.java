package com.thewizrd.weather_api.aqicn;

import com.google.gson.annotations.SerializedName;

public class AttributionsItem {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}