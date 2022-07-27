package com.thewizrd.weather_api.nws.alerts;

import com.squareup.moshi.Json;

//@JsonClass(generateAdapter = true, generator = "java")
public class Context {

    @Json(name = "wx")
    private String wx;

    @Json(name = "@vocab")
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