package com.thewizrd.simpleweather.weather.yahoo.data;

import com.google.gson.annotations.SerializedName;

public class Item
{
    public String title;
    public String lat;
    @SerializedName("long")
    public String _long;
    public String link;
    public String pubDate;
    public Condition condition;
    public Forecast[] forecast;
    public String description;
    public Guid guid;
}
