package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class SnowDay {

    @SerializedName("in")
    private String in;

    @SerializedName("cm")
    private String cm;

    public void setCm(float cm) {
        this.cm = Float.toString(cm);
    }

    public float getCm() {
        float result = 0;
        try {
            result = Float.parseFloat(cm);
        } catch (NumberFormatException e) {
            // Do nothing
        }
        return result;
    }

    public void setIn(float in) {
        this.in = Float.toString(in);
    }

    public float getIn() {
        float result = 0.0f;
        try {
            result = Float.parseFloat(in);
        } catch (NumberFormatException e) {
            // Do nothing
        }
        return result;
    }
}