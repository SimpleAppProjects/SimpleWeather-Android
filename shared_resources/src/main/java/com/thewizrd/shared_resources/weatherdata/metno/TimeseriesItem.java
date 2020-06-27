package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class TimeseriesItem {

    @SerializedName("data")
    private Data data;

    @SerializedName("time")
    private String time;

    public void setData(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }
}