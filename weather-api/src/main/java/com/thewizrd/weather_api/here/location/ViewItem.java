package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class ViewItem {

    @Json(name = "_type")
    private String type;

    @Json(name = "viewId")
    private int viewId;

    @Json(name = "result")
    private List<ResultItem> result;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    public int getViewId() {
        return viewId;
    }

    public void setResult(List<ResultItem> result) {
        this.result = result;
    }

    public List<ResultItem> getResult() {
        return result;
    }
}