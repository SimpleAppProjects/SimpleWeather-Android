package com.thewizrd.shared_resources.locationdata;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.util.Collection;

public interface LocationProviderImplInterface {
    @WeatherAPI.LocationAPIs
    String getLocationAPI();

    boolean isKeyRequired();

    boolean supportsLocale();

    boolean needsLocationFromID();

    boolean needsLocationFromName();

    boolean needsLocationFromGeocoder();

    Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException;

    LocationQueryViewModel getLocation(@NonNull WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException;

    LocationQueryViewModel getLocationFromID(@NonNull LocationQueryViewModel model) throws WeatherException;

    LocationQueryViewModel getLocationFromName(@NonNull LocationQueryViewModel model) throws WeatherException;

    boolean isKeyValid(String key) throws WeatherException;

    String getAPIKey();

    String localeToLangCode(String iso, String name);

    void updateLocationData(LocationData location, String weatherAPI);

}
