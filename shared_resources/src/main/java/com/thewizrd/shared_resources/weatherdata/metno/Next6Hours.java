package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Next6Hours {

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("details")
    private Details3 details;

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setDetails(Details3 details) {
        this.details = details;
    }

    public Details3 getDetails() {
        return details;
    }
}