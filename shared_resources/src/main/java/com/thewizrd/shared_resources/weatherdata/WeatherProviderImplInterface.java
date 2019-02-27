package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.util.Collection;
import java.util.List;

public interface WeatherProviderImplInterface {
    String getWeatherAPI();

    boolean isKeyRequired();

    boolean supportsWeatherLocale();

    boolean supportsAlerts();

    boolean needsExternalAlertData();

    Collection<LocationQueryViewModel> getLocations(String ac_query);

    LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate);

    LocationQueryViewModel getLocation(String query);

    Weather getWeather(String location_query) throws WeatherException;

    Weather getWeather(LocationData location) throws WeatherException;

    List<WeatherAlert> getAlerts(LocationData location);

    String getWeatherIcon(String icon);

    String getWeatherIcon(boolean isNight, String icon);

    boolean isKeyValid(String key);

    String getAPIKey();

    boolean isNight(Weather weather);

    String localeToLangCode(String iso, String name);

    void updateLocationData(LocationData location);

    String updateLocationQuery(Weather weather);

    String updateLocationQuery(LocationData location);

    String getWeatherBackgroundURI(Weather weather);

    int getWeatherBackgroundColor(Weather weather);

    int getWeatherIconResource(String icon);

    LocationProviderImpl getLocationProvider();

}
