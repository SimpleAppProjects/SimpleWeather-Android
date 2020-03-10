package com.thewizrd.shared_resources.weatherdata.nws.alerts;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class AlertRootobject {

    @SerializedName("@graph")
    private List<GraphItem> graph;

    @SerializedName("title")
    private String title;

    @SerializedName("@context")
    private Context context;

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

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}