package com.thewizrd.shared_resources.locationdata;

import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.util.Collection;

public interface LocationProviderImplInterface {
    String getLocationAPI();

    boolean isKeyRequired();

    boolean supportsLocale();

    Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException;

    LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException;

    boolean isKeyValid(String key) throws WeatherException;

    String getAPIKey();

    String localeToLangCode(String iso, String name);

    void updateLocationData(LocationData location, String weatherAPI);

}
