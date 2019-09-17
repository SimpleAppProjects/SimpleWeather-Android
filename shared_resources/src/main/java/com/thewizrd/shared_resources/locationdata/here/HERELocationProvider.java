package com.thewizrd.shared_resources.locationdata.here;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.BuildConfig;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

public final class HERELocationProvider extends LocationProviderImpl {

    @Override
    public String getLocationAPI() {
        return WeatherAPI.HERE;
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

        String queryAPI = "https://autocomplete.geocoder.api.here.com/6.2/suggest.json";
        if (BuildConfig.DEBUG)
            queryAPI = "https://autocomplete.geocoder.cit.api.here.com/6.2/suggest.json";

        String query = "?query=%s&app_id=%s&app_code=%s&language=%s&maxresults=10";
        HttpURLConnection client = null;
        WeatherException wEx = null;
        // Limit amount of results shown
        int maxResults = 10;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String app_id = getAppID();
        String app_code = getAppCode();

        try {
            // Connect to webstream
            URL queryURL = new URL(String.format(queryAPI + query, URLEncoder.encode(ac_query, "UTF-8"), app_id, app_code, locale));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            InputStream stream = client.getInputStream();

            // Load data
            locations = new HashSet<>(); // Use HashSet to avoid duplicate location (names)
            AutoCompleteQuery root = JSONParser.deserializer(stream, AutoCompleteQuery.class);

            for (SuggestionsItem result : root.getSuggestions()) {
                boolean added = false;
                // Filter: only store city results
                if ("city".equals(result.getMatchLevel())
                        || "district".equals(result.getMatchLevel())
                        || "postalCode".equals(result.getMatchLevel()))
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
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting locations");
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

        String queryAPI = "https://reverse.geocoder.cit.api.here.com/6.2/reversegeocode.json";
        if (BuildConfig.DEBUG)
            queryAPI = "https://reverse.geocoder.api.here.com/6.2/reversegeocode.json";

        String location_query = String.format(Locale.ROOT, "%s,%s", Double.toString(coord.getLatitude()), Double.toString(coord.getLongitude()));
        String query = "?prox=%s,150&mode=retrieveAddresses&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
                "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s&app_id=%s&app_code=%s";
        HttpURLConnection client = null;
        ResultItem result = null;
        WeatherException wEx = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String app_id = getAppID();
        String app_code = getAppCode();

        try {
            // Connect to webstream
            URL queryURL = new URL(String.format(queryAPI + query, location_query, locale, app_id, app_code));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            InputStream stream = client.getInputStream();

            // Load data
            Geo_Rootobject root = JSONParser.deserializer(stream, Geo_Rootobject.class);

            if (root.getResponse().getView().size() > 0 && root.getResponse().getView().get(0).getResult().size() > 0)
                result = root.getResponse().getView().get(0).getResult().get(0);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting location");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null)
            throw wEx;

        if (result != null && !StringUtils.isNullOrWhitespace(result.getLocation().getLocationId()))
            location = new LocationQueryViewModel(result, weatherAPI);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    public LocationQueryViewModel getLocationfromLocID(String locationID, String weatherAPI) throws WeatherException {
        LocationQueryViewModel location = null;

        String queryAPI = "https://geocoder.api.here.com/6.2/geocode.json";
        if (BuildConfig.DEBUG)
            queryAPI = "https://geocoder.cit.api.here.com/6.2/geocode.json";

        String query = "?locationid=%s&mode=retrieveAddresses&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
                "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s&app_id=%s&app_code=%s";
        HttpURLConnection client = null;
        ResultItem result = null;
        WeatherException wEx = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String app_id = getAppID();
        String app_code = getAppCode();

        try {
            // Connect to webstream
            URL queryURL = new URL(String.format(queryAPI + query, locationID, locale, app_id, app_code));
            client = (HttpURLConnection) queryURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            InputStream stream = client.getInputStream();

            // Load data
            Geo_Rootobject root = JSONParser.deserializer(stream, Geo_Rootobject.class);

            if (root.getResponse().getView().size() > 0 && root.getResponse().getView().get(0).getResult().size() > 0)
                result = root.getResponse().getView().get(0).getResult().get(0);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting location");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null)
            throw wEx;

        if (result != null && !StringUtils.isNullOrWhitespace(result.getLocation().getLocationId()))
            location = new LocationQueryViewModel(result, weatherAPI);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public boolean isKeyValid(String key) throws WeatherException {
        String queryAPI = "https://weather.cit.api.here.com/weather/1.0/report.json";

        String app_id = "";
        String app_code = "";

        if (!StringUtils.isNullOrWhitespace(key)) {
            String[] keyArr = key.split(";");
            if (keyArr.length > 0) {
                app_id = keyArr[0];
                app_code = keyArr[keyArr.length > 1 ? keyArr.length - 1 : 0];
            }
        }

        HttpURLConnection client = null;
        boolean isValid = false;
        WeatherException wEx = null;

        try {
            if (StringUtils.isNullOrWhitespace(app_id) || StringUtils.isNullOrWhitespace(app_code)) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                throw wEx;
            }

            // Connect to webstream
            URL queryURL = new URL(String.format("%s?app_id=%s&app_code=%s", queryAPI, app_id, app_code));
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

    private String getAppID() {
        return Keys.getHEREAppID();
    }

    private String getAppCode() {
        return Keys.getHEREAppCode();
    }

    @Override
    public String getAPIKey() {
        if (StringUtils.isNullOrWhitespace(getAppID()) && StringUtils.isNullOrWhitespace(getAppCode()))
            return null;
        else
            return String.format("%s;%s", getAppID(), getAppCode());
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return name;
    }

}
