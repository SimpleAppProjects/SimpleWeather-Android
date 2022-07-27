package com.thewizrd.weather_api.nws.observation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Time {

    @Json(name = "startValidTime")
    private List<String> startValidTime;

    @Json(name = "layoutKey")
    private String layoutKey;

    @Json(name = "startPeriodName")
    private List<String> startPeriodName;

    @Json(name = "tempLabel")
    private List<String> tempLabel;

    public void setStartValidTime(List<String> startValidTime) {
        this.startValidTime = startValidTime;
    }

    public List<String> getStartValidTime() {
        return startValidTime;
    }

    public void setLayoutKey(String layoutKey) {
        this.layoutKey = layoutKey;
    }

    public String getLayoutKey() {
        return layoutKey;
    }

    public void setStartPeriodName(List<String> startPeriodName) {
        this.startPeriodName = startPeriodName;
    }

    public List<String> getStartPeriodName() {
        return startPeriodName;
    }

    public void setTempLabel(List<String> tempLabel) {
        this.tempLabel = tempLabel;
    }

    public List<String> getTempLabel() {
        return tempLabel;
    }
}