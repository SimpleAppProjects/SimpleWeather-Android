package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Properties {

    @SerializedName("timeseries")
    private List<TimeseriesItem> timeseries;

    @SerializedName("meta")
    private Meta meta;

    public void setTimeseries(List<TimeseriesItem> timeseries) {
        this.timeseries = timeseries;
    }

    public List<TimeseriesItem> getTimeseries() {
        return timeseries;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Meta getMeta() {
        return meta;
    }
}