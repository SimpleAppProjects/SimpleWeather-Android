package com.thewizrd.simpleweather.weather.weatherunderground.data;

import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_RESULT;

public class AC_Location
{
    public String name;
    public String c;
    public String zmw;
    public String l;
    public String lat;
    public String lon;

    public AC_Location(AC_RESULT result)
    {
        name = result.name;
        c = result.c;
        zmw = result.zmw;
        l = result.l;
        lat = result.lat;
        lon = result.lon;
    }
}
