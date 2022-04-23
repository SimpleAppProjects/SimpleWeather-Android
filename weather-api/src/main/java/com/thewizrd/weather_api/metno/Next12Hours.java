package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Next12Hours {

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("details")
    private Details1 details;

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setDetails(Details1 details) {
        this.details = details;
    }

    public Details1 getDetails() {
        return details;
    }
}