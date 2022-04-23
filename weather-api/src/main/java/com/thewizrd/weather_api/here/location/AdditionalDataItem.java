package com.thewizrd.weather_api.here.location;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class AdditionalDataItem {

    @SerializedName("value")
    private String value;

    @SerializedName("key")
    private String key;

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}