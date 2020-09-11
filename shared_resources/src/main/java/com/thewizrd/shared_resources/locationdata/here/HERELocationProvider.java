package com.thewizrd.shared_resources.locationdata.here;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class HERELocationProvider extends LocationProviderImpl {
    private static final String AUTOCOMPLETE_QUERY_URL = "https://autocomplete.geocoder.ls.hereapi.com/6.2/suggest.json?query=%s&language=%s&maxresults=10";
    private static final String GEOLOCATION_QUERY_URL = "https://reverse.geocoder.ls.hereapi.com/6.2/reversegeocode.json" +
            "?prox=%s,150&mode=retrieveAddresses&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
            "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s";
    private static final String GEOCODER_QUERY_URL = "https://geocoder.ls.hereapi.com/6.2/geocode.json" +
            "?locationid=%s&mode=retrieveAddresses&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
            "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s";

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

        // Limit amount of results shown
        int maxResults = 10;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        WeatherException wEx = null;

        try {
            final String authorization = HEREOAuthUtils.getBearerToken(false);

            if (StringUtils.isNullOrWhitespace(authorization)) {
                throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            Request request = new Request.Builder()
                    .url(String.format(AUTOCOMPLETE_QUERY_URL, URLEncoder.encode(ac_query, "UTF-8"), locale))
                    .addHeader("Authorization", authorization)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

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
            if (response != null)
                response.close();
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

        String location_query = String.format(Locale.ROOT, "%s,%s", coord.getLatitude(), coord.getLongitude());

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        ResultItem result = null;
        WeatherException wEx = null;

        try {
            final String authorization = HEREOAuthUtils.getBearerToken(false);

            if (StringUtils.isNullOrWhitespace(authorization)) {
                throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            Request request = new Request.Builder()
                    .url(String.format(GEOLOCATION_QUERY_URL, location_query, locale))
                    .addHeader("Authorization", authorization)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

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
            if (response != null)
                response.close();
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

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        ResultItem result = null;
        WeatherException wEx = null;

        try {
            final String authorization = HEREOAuthUtils.getBearerToken(false);

            if (StringUtils.isNullOrWhitespace(authorization)) {
                throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            Request request = new Request.Builder()
                    .url(String.format(GEOCODER_QUERY_URL, locationID, locale))
                    .addHeader("Authorization", authorization)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

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
            if (response != null)
                response.close();
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
    public boolean isKeyValid(String key) {
        return false;
    }

    @Override
    public String getAPIKey() {
        return null;
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return name;
    }

}
