package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

//@UseStag(UseStag.FieldOption.ALL)
public class Elevation {

    @SerializedName("unitCode")
    private String unitCode;

    @SerializedName("value")
    private int value;

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}