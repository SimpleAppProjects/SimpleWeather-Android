package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class ForecastsItem {

    @SerializedName("date")
    private String date;

    @SerializedName("high")
    private String high;

    @SerializedName("code")
    private String code;

    @SerializedName("low")
    private String low;

    @SerializedName("text")
    private String text;

    @SerializedName("day")
    private String day;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getHigh() {
        return high;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public String getLow() {
        return low;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getDay() {
        return day;
    }
}