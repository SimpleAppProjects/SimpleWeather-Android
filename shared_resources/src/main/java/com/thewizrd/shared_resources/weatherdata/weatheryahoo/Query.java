package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class Query {

    @SerializedName("created")
    private String created;

    @SerializedName("count")
    private int count;

    @SerializedName("lang")
    private String lang;

    @SerializedName("results")
    private Results results;

    public void setCreated(String created) {
        this.created = created;
    }

    public String getCreated() {
        return created;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public Results getResults() {
        return results;
    }
}