package com.thewizrd.simpleweather.weather.yahoo.data;

import java.text.SimpleDateFormat;

public class Forecast
{
    public String code;
    private String date;
    public String day;
    public String high;
    public String low;
    public String text;

    public String getDate() {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
        try {
            return new SimpleDateFormat("EEEE dd").format(format.parse(date));
        } catch (Exception e) {
            return date;
        }
    }
}
