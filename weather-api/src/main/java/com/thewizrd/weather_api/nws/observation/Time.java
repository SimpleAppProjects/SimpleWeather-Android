package com.thewizrd.weather_api.nws.observation;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Time {

    @SerializedName("startValidTime")
    private List<String> startValidTime;

    @SerializedName("layoutKey")
    private String layoutKey;

    @SerializedName("startPeriodName")
    private List<String> startPeriodName;

    @SerializedName("tempLabel")
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