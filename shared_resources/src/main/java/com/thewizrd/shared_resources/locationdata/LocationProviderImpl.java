package com.thewizrd.shared_resources.locationdata;

import android.util.Log;

import com.skedgo.converter.TimezoneMapper;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.util.Collection;

public abstract class LocationProviderImpl implements LocationProviderImplInterface {
    // Variables
    public abstract String getLocationAPI();

    public abstract boolean isKeyRequired();

    public abstract boolean supportsLocale();

    // Methods
    // AutoCompleteQuery
    public abstract Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException;

    // GeopositionQuery
    public abstract LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException;

    // KeyCheck
    public abstract boolean isKeyValid(String key) throws WeatherException;

    public abstract String getAPIKey();

    // Utils Methods
    @Override
    public void updateLocationData(LocationData location, String weatherAPI) {
        LocationQueryViewModel qview = null;
        try {
            qview = getLocation(new WeatherUtils.Coordinate(location), weatherAPI);
        } catch (WeatherException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        if (qview != null && !StringUtils.isNullOrWhitespace(qview.getLocationQuery())) {
            location.setName(qview.getLocationName());
            location.setLatitude(qview.getLocationLat());
            location.setLongitude(qview.getLocationLong());
            location.setTzLong(qview.getLocationTZLong());
            if (StringUtils.isNullOrWhitespace(location.getTzLong()) && location.getLongitude() != 0 && location.getLatitude() != 0) {
                String tzId = TimezoneMapper.latLngToTimezoneString(location.getLatitude(), location.getLongitude());
                if (!"unknown".equals(tzId))
                    location.setTzLong(tzId);
            }
            location.setLocationSource(qview.getLocationSource());

            // Update DB here or somewhere else
            if (SimpleLibrary.getInstance().getApp().isPhone()) {
                Settings.updateLocation(location);
            } else {
                Settings.saveHomeData(location);
            }
        }
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return "EN";
    }

}
