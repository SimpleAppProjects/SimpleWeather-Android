package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Next1Hours {

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("details")
    private Details2 details;

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setDetails(Details2 details) {
        this.details = details;
    }

    public Details2 getDetails() {
        return details;
    }
}