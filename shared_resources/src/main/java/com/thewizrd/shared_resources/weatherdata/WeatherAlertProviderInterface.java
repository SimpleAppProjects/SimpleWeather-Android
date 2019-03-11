package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;

import java.util.List;

public interface WeatherAlertProviderInterface {
    List<WeatherAlert> getAlerts(LocationData location);
}
