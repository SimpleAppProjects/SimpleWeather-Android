package com.thewizrd.shared_resources.locationdata;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
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

    public abstract boolean needsLocationFromID();

    public abstract boolean needsLocationFromName();

    /**
     * Retrieve a list of locations from the location provider
     *
     * @param ac_query   The AutoComplete query used to search locations
     * @param weatherAPI The weather source to be assigned
     * @return A list of locations matching the query
     * @throws WeatherException Weather Exception
     */
    public abstract Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException;

    /**
     * Retrieve a single (geo)location from the location provider
     *
     * @param coordinate The coordinate used to search the location data
     * @param weatherAPI The weather source to be assigned
     * @return A single location matching the provided coordinate
     * @throws WeatherException Weather Exception
     */
    public abstract LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException;

    /**
     * Retrieve a single location using the location id
     *
     * @param locationID The location id for the location to be queried
     * @param weatherAPI The weather source to be assigned
     * @return A single location matching the provided location id
     * @throws WeatherException Weather Exception
     */
    public abstract LocationQueryViewModel getLocationFromID(String locationID, String weatherAPI) throws WeatherException;

    /**
     * Retrieve a single location using the location name
     *
     * @param locationName The location name for the location to be queried
     * @param weatherAPI   The weather source to be assigned
     * @return A single location matching the provided location id
     * @throws WeatherException Weather Exception
     */
    public abstract LocationQueryViewModel getLocationFromName(String locationName, String weatherAPI) throws WeatherException;

    /**
     * Query the location provider if the provided key is valid
     *
     * @param key Provider key to check
     * @return boolean Is valid or not
     * @throws WeatherException Weather Exception
     */
    public abstract boolean isKeyValid(String key) throws WeatherException;

    public abstract String getAPIKey();

    /**
     * Refresh/update the location data from the supported location provider
     * and commit update to the database
     *
     * Uses coordinate {@link LocationData#getLatitude()}, {@link LocationData#getLongitude()}
     * to query location provider for updated location data
     *
     * @param location Location data to update
     */
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
                String tzId = TZDBCache.getTimeZone(location.getLatitude(), location.getLongitude());
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

    /**
     * Returns the locale code supported by this location provider
     *
     * @param iso See {@link ULocale#getLanguage()}
     * @param name See {@link ULocale#toLanguageTag()}
     * @return The locale code supported by this provider
     */
    @Override
    public String localeToLangCode(String iso, String name) {
        return "EN";
    }
}
