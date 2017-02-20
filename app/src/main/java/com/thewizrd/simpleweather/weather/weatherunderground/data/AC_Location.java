package com.thewizrd.simpleweather.weather.weatherunderground.data;

import com.thewizrd.simpleweather.weather.weatherunderground.GeopositionQuery;

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

    public AC_Location(GeopositionQuery.location result)
    {
        name = String.format("%s, %s", result.city, result.state);
        c = result.country;
        zmw = String.format("%s.%s.%s", result.zip, result.magic, result.wmo);
        l = String.format("/q/zmw:%s", zmw);
        lat = result.lat;
        lon = result.lon;
    }
}
