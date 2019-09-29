package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

public class ObservationStations {

    @SerializedName("@type")
    private String type;

    @SerializedName("@container")
    private String container;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getContainer() {
        return container;
    }
}