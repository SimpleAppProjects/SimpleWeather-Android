package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;

import java.util.Collection;

public interface WeatherAlertProviderInterface {
    Collection<WeatherAlert> getAlerts(LocationData location);
}
