package com.thewizrd.weather_api.nws.alerts;

import com.google.gson.annotations.SerializedName;

//@UseStag(UseStag.FieldOption.ALL)
public class Context {

    @SerializedName("wx")
    private String wx;

    @SerializedName("@vocab")
    private String vocab;

    public void setWx(String wx) {
        this.wx = wx;
    }

    public String getWx() {
        return wx;
    }

    public void setVocab(String vocab) {
        this.vocab = vocab;
    }

    public String getVocab() {
        return vocab;
    }
}