package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.WeatherException;

public interface AirQualityProviderInterface {
    AirQuality getAirQualityData(LocationData location) throws WeatherException;
}
