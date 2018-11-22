package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class Condition {

    @SerializedName("date")
    private String date;

    @SerializedName("temp")
    private String temp;

    @SerializedName("code")
    private String code;

    @SerializedName("text")
    private String text;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getTemp() {
        return temp;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}