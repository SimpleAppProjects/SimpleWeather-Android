package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

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