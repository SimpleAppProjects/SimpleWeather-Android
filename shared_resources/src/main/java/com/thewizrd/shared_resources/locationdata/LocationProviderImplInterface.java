package com.thewizrd.shared_resources.locationdata;

import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;

import java.util.Collection;

public interface LocationProviderImplInterface {
    String getLocationAPI();

    boolean isKeyRequired();

    boolean supportsLocale();

    Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI);

    LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate, String weatherAPI);

    LocationQueryViewModel getLocation(String query, String weatherAPI);

    boolean isKeyValid(String key);

    String getAPIKey();

    String localeToLangCode(String iso, String name);

    void updateLocationData(LocationData location, String weatherAPI);

}
