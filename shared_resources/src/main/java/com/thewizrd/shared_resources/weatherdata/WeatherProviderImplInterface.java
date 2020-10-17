package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.util.Collection;

public interface WeatherProviderImplInterface {
    String getWeatherAPI();

    boolean isKeyRequired();

    boolean supportsWeatherLocale();

    boolean supportsAlerts();

    boolean needsExternalAlertData();

    Collection<LocationQueryViewModel> getLocations(String ac_query) throws WeatherException;

    LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate) throws WeatherException;

    Weather getWeather(String location_query, String country_code) throws WeatherException;

    Weather getWeather(LocationData location) throws WeatherException;

    Collection<WeatherAlert> getAlerts(LocationData location);

    String getWeatherIcon(String icon);

    String getWeatherIcon(boolean isNight, String icon);

    String getWeatherCondition(String icon);

    boolean isKeyValid(String key) throws WeatherException;

    String getAPIKey();

    boolean isNight(Weather weather);

    String localeToLangCode(String iso, String name);

    void updateLocationData(LocationData location);

    String updateLocationQuery(Weather weather);

    String updateLocationQuery(LocationData location);

    LocationProviderImpl getLocationProvider();

}
