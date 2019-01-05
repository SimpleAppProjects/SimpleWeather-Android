package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

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