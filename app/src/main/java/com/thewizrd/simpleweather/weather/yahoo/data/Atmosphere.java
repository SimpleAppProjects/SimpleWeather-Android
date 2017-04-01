package com.thewizrd.simpleweather.weather.yahoo.data;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.simpleweather.utils.ConversionMethods;

public class Atmosphere
{
    @SerializedName("humidity")
    private String _humidity;
    @SerializedName("pressure")
    private String _pressure;
    public String rising;
    @SerializedName("visibility")
    private String _visibility;

    public String getHumidity() {
        return _humidity + "%";
    }

    public void setHumidity(String value) {
        _humidity = value;
    }

    public String getPressure() {
        return ConversionMethods.mbToInHg(_pressure);
    }

    public void setPressure(String value) {
        _pressure = value;
    }

    public String getVisibility() {
        return ConversionMethods.kmToMi(_visibility);
    }

    public void setVisibility(String value) {
        _visibility = value;
    }
}
