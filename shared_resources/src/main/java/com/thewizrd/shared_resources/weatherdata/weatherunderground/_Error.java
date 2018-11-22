package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class _Error {

    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
