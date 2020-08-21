package com.thewizrd.shared_resources.locationdata.locationiq;

import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class LocationIQProvider extends LocationProviderImpl {

    @Override
    public String getLocationAPI() {
        return WeatherAPI.LOCATIONIQ;
    }

    @Override
    public boolean supportsLocale() {
        return true;
    }

    @Override
    public boolean isKeyRequired() {
        return false;
    }

    @Override
    public Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException {
        Collection<LocationQueryViewModel> locations = null;

        String queryAPI = "https://api.locationiq.com/v1/autocomplete.php";
        String query = "?key=%s&q=%s&limit=10&normalizecity=1&addressdetails=1&accept-language=%s";
        HttpURLConnection client = null;
        WeatherException wEx = null;
        // Limit amount of results shown
        int maxResults = 10;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = getAPIKey();

        try {
            // Connect to webstream
            URL queryURL = new URL(String.format(queryAPI + query, key, URLEncoder.encode(ac_query, "UTF-8"), locale));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            InputStream stream = client.getInputStream();

            // Load data
            locations = new HashSet<>(); // Use HashSet to avoid duplicate location (names)
            Type arrListType = new TypeToken<ArrayList<AutoCompleteQuery>>() {
            }.getType();
            List<AutoCompleteQuery> root = JSONParser.deserializer(stream, arrListType);

            for (AutoCompleteQuery result : root) {
                boolean added = false;
                // Filter: only store city results
                if ("place".equals(result.getJsonMemberClass()))
                    added = locations.add(new LocationQueryViewModel(result, weatherAPI));
                else
                    continue;

                // Limit amount of results
                if (added) {
                    maxResults--;
                    if (maxResults <= 0)
                        break;
                }
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "LocationIQProvider: error getting locations");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null)
            throw wEx;

        if (locations == null || locations.size() == 0) {
            locations = Collections.singletonList(new LocationQueryViewModel());
        }

        return locations;
    }

    @Override
    public LocationQueryViewModel getLocation(WeatherUtils.Coordinate coord, String weatherAPI) throws WeatherException {
        LocationQueryViewModel location = null;

        String queryAPI = "https://api.locationiq.com/v1/reverse.php";
        String query = "?key=%s&lat=%s&lon=%s&format=json&zoom=14&namedetails=0&addressdetails=1&accept-language=%s&normalizecity=1";
        HttpURLConnection client = null;
        GeoLocation result = null;
        WeatherException wEx = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = getAPIKey();

        try {
            // Connect to webstream
            URL queryURL = new URL(String.format(queryAPI + query, key, Double.toString(coord.getLatitude()), Double.toString(coord.getLongitude()), locale));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            InputStream stream = client.getInputStream();

            // Load data
            result = JSONParser.deserializer(stream, GeoLocation.class);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "LocationIQProvider: error getting location");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null)
            throw wEx;

        if (result != null && !StringUtils.isNullOrWhitespace(result.getOsmId()))
            location = new LocationQueryViewModel(result, weatherAPI);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public boolean isKeyValid(String key) throws WeatherException {
        String queryAPI = "https://us1.unwiredlabs.com/v2/timezone.php";

        HttpURLConnection client = null;
        boolean isValid = false;
        WeatherException wEx = null;

        try {
            if (StringUtils.isNullOrWhitespace(key)) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                throw wEx;
            }

            // Connect to webstream
            URL queryURL = new URL(String.format("%s?token=%s", queryAPI, key));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Check for errors
            switch (client.getResponseCode()) {
                // 400 (OK since this isn't a valid request)
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    isValid = true;
                    break;
                // 401 (Unauthorized - Key is invalid)
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                    isValid = false;
                    break;
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            isValid = false;
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null) {
            throw wEx;
        }

        return isValid;
    }

    @Override
    public String getAPIKey() {
        return Keys.getLocIQKey();
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return iso;
    }

}
