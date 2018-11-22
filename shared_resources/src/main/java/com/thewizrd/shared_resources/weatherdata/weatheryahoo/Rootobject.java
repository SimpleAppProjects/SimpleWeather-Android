package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class Rootobject {

    @SerializedName("query")
    private Query query;

    public void setQuery(Query query) {
        this.query = query;
    }

    public Query getQuery() {
        return query;
    }
}