package com.thewizrd.simpleweather.weather.yahoo.data;

public class Location
{
    public String city;
    public String country;
    public String region;

    public String lat;
    public String _long;

    public int offset;

    public String getDescription() {
        return city + "," + region;
    }
}
