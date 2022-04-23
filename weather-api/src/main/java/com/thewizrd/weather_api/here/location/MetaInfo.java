package com.thewizrd.weather_api.here.location;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class MetaInfo {

    @SerializedName("nextPageInformation")
    private String nextPageInformation;

    @SerializedName("timestamp")
    private String timestamp;

    public void setNextPageInformation(String nextPageInformation) {
        this.nextPageInformation = nextPageInformation;
    }

    public String getNextPageInformation() {
        return nextPageInformation;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }
}