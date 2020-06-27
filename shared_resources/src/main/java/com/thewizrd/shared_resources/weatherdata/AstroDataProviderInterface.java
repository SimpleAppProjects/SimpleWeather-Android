package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.WeatherException;

public interface AstroDataProviderInterface {
    Astronomy getAstronomyData(LocationData location) throws WeatherException;
}

