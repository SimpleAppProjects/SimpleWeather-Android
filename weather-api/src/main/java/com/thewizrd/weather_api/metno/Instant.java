package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Instant {

    @SerializedName("details")
    private Details details;

    public void setDetails(Details details) {
        this.details = details;
    }

    public Details getDetails() {
        return details;
    }
}