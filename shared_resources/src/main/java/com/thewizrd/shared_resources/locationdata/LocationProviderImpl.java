package com.thewizrd.shared_resources.locationdata;

import android.os.Handler;
import android.os.Looper;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import java.util.Collection;

public abstract class LocationProviderImpl implements LocationProviderImplInterface {
    protected final Handler mMainHandler;

    public LocationProviderImpl() {
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    // Variables
    public abstract String getLocationAPI();

    public abstract boolean isKeyRequired();

    public abstract boolean supportsLocale();

    // Methods
    // AutoCompleteQuery
    public abstract Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI);

    // GeopositionQuery
    public abstract LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate, String weatherAPI);

    // KeyCheck
    public abstract boolean isKeyValid(String key);

    public abstract String getAPIKey();

    // Utils Methods
    @Override
    public void updateLocationData(LocationData location, String weatherAPI) {
        LocationQueryViewModel qview = getLocation(new WeatherUtils.Coordinate(location), weatherAPI);

        if (qview != null && !StringUtils.isNullOrWhitespace(qview.getLocationQuery())) {
            location.setName(qview.getLocationName());
            location.setLatitude(qview.getLocationLat());
            location.setLongitude(qview.getLocationLong());
            location.setTzLong(qview.getLocationTZLong());

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
