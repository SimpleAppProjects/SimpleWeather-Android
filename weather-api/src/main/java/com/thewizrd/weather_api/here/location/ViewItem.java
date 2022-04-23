package com.thewizrd.weather_api.here.location;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ViewItem {

    @SerializedName("_type")
    private String type;

    @SerializedName("viewId")
    private int viewId;

    @SerializedName("result")
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