package com.thewizrd.simpleweather.weather.yahoo.data;

import java.text.SimpleDateFormat;

public class Astronomy
{
    private String sunrise;
    private String sunset;

    private transient SimpleDateFormat format = new SimpleDateFormat("hh:mm aa");

    public String getSunrise() {
        try {
            return format.format(format.parse(sunrise));
        } catch (Exception e) {
            return sunrise;
        }
    }

    public String getSunset() {
        try {
            return format.format(format.parse(sunset));
        } catch (Exception e) {
            return sunrise;
        }
    }
}
