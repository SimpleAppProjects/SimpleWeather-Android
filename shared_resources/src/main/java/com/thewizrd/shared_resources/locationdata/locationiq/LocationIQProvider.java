package com.thewizrd.shared_resources.locationdata.locationiq;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.ExceptionUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class LocationIQProvider extends LocationProviderImpl {
    private static final String AUTOCOMPLETE_QUERY_URL = "https://api.locationiq.com/v1/autocomplete.php?key=%s&q=%s&limit=10&normalizecity=1&addressdetails=1&accept-language=%s";
    private static final String GEOLOCATION_QUERY_URL = "https://api.locationiq.com/v1/reverse.php?key=%s&lat=%s&lon=%s&format=json&zoom=14&namedetails=0&addressdetails=1&accept-language=%s&normalizecity=1";
    private static final String KEY_QUERY_URL = "https://us1.unwiredlabs.com/v2/timezone.php?token=%s";

    @WeatherAPI.LocationAPIs
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

        // Limit amount of results shown
        int maxResults = 10;

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = getAPIKey();

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        WeatherException wEx = null;

        try {
            Request request = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .get()
                    .url(String.format(AUTOCOMPLETE_QUERY_URL, key, URLEncoder.encode(ac_query, "UTF-8"), locale))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

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
    public LocationQueryViewModel getLocation(@NonNull WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException {
        LocationQueryViewModel location = super.getLocation(coordinate, weatherAPI);

        if (location != null) {
            return location;
        }

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = getAPIKey();

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        GeoLocation result = null;
        WeatherException wEx = null;

        try {
            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
            df.applyPattern("0.####");

            Request request = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .get()
                    .url(String.format(Locale.ROOT, GEOLOCATION_QUERY_URL, key, df.format(coordinate.getLatitude()), df.format(coordinate.getLongitude()), locale))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

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
            if (response != null)
                response.close();
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
    public LocationQueryViewModel getLocationFromID(@NonNull LocationQueryViewModel model) throws WeatherException {
        return null;
    }

    @Override
    public LocationQueryViewModel getLocationFromName(@NonNull LocationQueryViewModel model) throws WeatherException {
        return null;
    }

    @Override
    public boolean isKeyValid(String key) throws WeatherException {
        if (StringUtils.isNullOrWhitespace(key)) {
            throw new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
        }

        boolean isValid = false;
        WeatherException wEx = null;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Request request = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(String.format(KEY_QUERY_URL, key))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            switch (response.code()) {
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
                wEx = ExceptionUtils.copyStackTrace(new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR), ex);
            }

            isValid = false;
        } finally {
            if (response != null)
                response.close();
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
