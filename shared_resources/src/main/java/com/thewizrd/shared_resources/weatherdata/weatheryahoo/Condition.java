package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Condition {

    @SerializedName("code")
    private String code;

    @SerializedName("temperature")
    private String temperature;

    @SerializedName("text")
    private String text;

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}