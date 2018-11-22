package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Low {

    @SerializedName("celsius")
    private String celsius;

    @SerializedName("fahrenheit")
    private String fahrenheit;

    public void setCelsius(String celsius) {
        this.celsius = celsius;
    }

    public String getCelsius() {
        return celsius;
    }

    public void setFahrenheit(String fahrenheit) {
        this.fahrenheit = fahrenheit;
    }

    public String getFahrenheit() {
        return fahrenheit;
    }
}