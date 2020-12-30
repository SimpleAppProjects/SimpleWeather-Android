package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.WeatherException;

import java.time.ZonedDateTime;

public interface AstroDataProviderDateInterface {
    Astronomy getAstronomyData(LocationData location, ZonedDateTime date) throws WeatherException;
}
