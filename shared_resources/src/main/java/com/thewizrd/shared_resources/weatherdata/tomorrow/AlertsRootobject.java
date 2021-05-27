package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class AlertsRootobject {

    @SerializedName("data")
    private AlertsData data;

    public void setData(AlertsData data) {
        this.data = data;
    }

    public AlertsData getData() {
        return data;
    }
}