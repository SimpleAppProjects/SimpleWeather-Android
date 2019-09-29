package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

public class PublicZone {

    @SerializedName("@type")
    private String type;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}