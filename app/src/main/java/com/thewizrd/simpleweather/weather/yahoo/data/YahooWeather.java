package com.thewizrd.simpleweather.weather.yahoo.data;

public class YahooWeather
{
    public String created;
    public String lastBuildDate;
    public Units units;
    public Location location;
    public Wind wind;
    public Atmosphere atmosphere;
    public Astronomy astronomy;
    public Condition condition;
    public Forecast[] forecasts;

    public String ttl;

    public YahooWeather(Rootobject root)
    {
        created = root.query.created;
        lastBuildDate = root.query.results.channel.lastBuildDate;
        units = root.query.results.channel.units;

        location = root.query.results.channel.location;
        location.lat = root.query.results.channel.item.lat;
        location._long = root.query.results.channel.item._long;

        wind = root.query.results.channel.wind;
        atmosphere = root.query.results.channel.atmosphere;
        astronomy = root.query.results.channel.astronomy;
        condition = root.query.results.channel.item.condition;
        forecasts = root.query.results.channel.item.forecast;

        ttl = root.query.results.channel.ttl;
    }
}