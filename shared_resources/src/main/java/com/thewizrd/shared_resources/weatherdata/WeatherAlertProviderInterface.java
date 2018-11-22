package com.thewizrd.shared_resources.weatherdata;

import java.util.List;

public interface WeatherAlertProviderInterface {
    List<WeatherAlert> getAlerts(LocationData location);
}
