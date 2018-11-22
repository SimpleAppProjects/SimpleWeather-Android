package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class QpfNight {

    @SerializedName("mm")
    private String mm;

    @SerializedName("in")
    private String in;

    public void setMm(int mm) {
        this.mm = Integer.toString(mm);
    }

    public int getMm() {
        int result = 0;
        try {
            result = Integer.parseInt(mm);
        } catch (NumberFormatException e) {
            // Do nothing
        }
        return result;
    }

    public void setIn(float in) {
        this.in = Float.toString(in);
    }

    public double getIn() {
        float result = 0.0f;
        try {
            result = Float.parseFloat(mm);
        } catch (NumberFormatException e) {
            // Do nothing
        }
        return result;
    }
}