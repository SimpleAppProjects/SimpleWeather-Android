package com.thewizrd.simpleweather.weather.yahoo.data;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.simpleweather.utils.ConversionMethods;

public class Wind
{
    public String chill;
    public String direction;

    @SerializedName("speed")
    private String _speed;

    public String getSpeed() {
        return ConversionMethods.kphTomph(_speed);
    }
}
