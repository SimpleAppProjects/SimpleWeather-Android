package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Response {

    @Json(name = "metaInfo")
    private MetaInfo metaInfo;

    @Json(name = "view")
    private List<ViewItem> view;

    public void setMetaInfo(MetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setView(List<ViewItem> view) {
        this.view = view;
    }

    public List<ViewItem> getView() {
        return view;
    }
}