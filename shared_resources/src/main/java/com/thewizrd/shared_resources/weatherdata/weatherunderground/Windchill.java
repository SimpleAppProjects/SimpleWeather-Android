package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Windchill {

    @SerializedName("metric")
    private String metric;

    @SerializedName("english")
    private String english;

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getMetric() {
        return metric;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getEnglish() {
        return english;
    }
}