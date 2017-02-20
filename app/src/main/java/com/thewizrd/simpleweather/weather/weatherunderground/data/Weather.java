package com.thewizrd.simpleweather.weather.weatherunderground.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Weather {
    public Location location;
    public java.util.Date update_time;
    public Current_Observation condition;
    public Simpleforecast forecast;
    public Sun_Phase sun_phase;

    public Weather(Rootobject root) {
        condition = root.current_observation;
        forecast = root.forecast.simpleforecast;
        sun_phase = root.sun_phase;
        try {
            update_time = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").parse(root.current_observation.local_time_rfc822);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        location = new Location();
        location.full_name = condition.display_location.full;
        location.city = condition.display_location.city;
        location.state = condition.display_location.state;
        location.state_name = condition.display_location.state_name;
        location.country = condition.display_location.country;
        location.zip = condition.display_location.zip;
        location.latitude = condition.display_location.latitude;
        location.longitude = condition.display_location.longitude;
        location.tz_offset = condition.local_tz_offset;
    }
}

