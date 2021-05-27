package com.thewizrd.shared_resources.weatherdata.ambee;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class DataItem {

    @SerializedName("Risk")
    private Risk risk;

    @SerializedName("Count")
    private Count count;

    @SerializedName("updatedAt")
    private String updatedAt;

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setCount(Count count) {
        this.count = count;
    }

    public Count getCount() {
        return count;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}