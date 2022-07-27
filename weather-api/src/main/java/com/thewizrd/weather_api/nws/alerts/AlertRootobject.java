package com.thewizrd.weather_api.nws.alerts;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class AlertRootobject {

    @Json(name = "@graph")
    private List<GraphItem> graph;

    @Json(name = "title")
    private String title;

    //@Json(name = "@context")
    //private Context context;

    public void setGraph(List<GraphItem> graph) {
        this.graph = graph;
    }

    public List<GraphItem> getGraph() {
        return graph;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /*
    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
     */
}