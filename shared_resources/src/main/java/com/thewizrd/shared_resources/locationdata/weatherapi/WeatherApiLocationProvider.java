package com.thewizrd.shared_resources.locationdata.weatherapi;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class WeatherApiLocationProvider extends LocationProviderImpl {
    private static final String QUERY_URL = "https://api.weatherapi.com/v1/search.json?key=%s&q=%s&lang=%s";

    @WeatherAPI.LocationAPIs
    @Override
    public String getLocationAPI() {
        return WeatherAPI.WEATHERAPI;
    }

    @Override
    public boolean isKeyRequired() {
        return false;
    }

    @Override
    public boolean supportsLocale() {
        return true;
    }

    @Override
    public boolean needsLocationFromName() {
        return true;
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
                    .get()
                    .url(String.format(QUERY_URL, key, URLEncoder.encode(ac_query, "UTF-8"), locale))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load data
            locations = new HashSet<>(); // Use HashSet to avoid duplicate location (names)
            Type arrListType = new TypeToken<ArrayList<LocationItem>>() {
            }.getType();
            List<LocationItem> root = JSONParser.deserializer(stream, arrListType);

            for (LocationItem result : root) {
                boolean added = locations.add(new LocationQueryViewModel(result, weatherAPI));

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
            Logger.writeLine(Log.ERROR, ex, "WeatherApiLocationProvider: error getting locations");
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

    public LocationQueryViewModel getLocationFromID(@NonNull LocationQueryViewModel model) throws WeatherException {
        return null;
    }

    public LocationQueryViewModel getLocationFromName(@NonNull LocationQueryViewModel model) throws WeatherException {
        return super.getLocationFromName(model);
    }

    @Override
    public LocationQueryViewModel getLocation(@NonNull WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException {
        LocationQueryViewModel location;
        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = getAPIKey();

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        LocationItem result = null;
        WeatherException wEx = null;

        try {
            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
            df.applyPattern("0.##");

            Request request = new Request.Builder()
                    .get()
                    .url(String.format(Locale.ROOT, QUERY_URL, key, String.format("%s,%s", df.format(coordinate.getLatitude()), df.format(coordinate.getLongitude())), locale))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load data
            Type arrListType = new TypeToken<ArrayList<LocationItem>>() {
            }.getType();
            List<LocationItem> locations = JSONParser.deserializer(stream, arrListType);

            for (LocationItem item : locations) {
                if (Math.abs(ConversionMethods.calculateHaversine(coordinate.getLatitude(), coordinate.getLongitude(), item.getLat(), item.getLon())) <= 100) {
                    result = item;
                    break;
                }
            }

            if (result == null) {
                result = locations.get(0);
            }

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

        if (result != null)
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
        return Keys.getWeatherApiKey();
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        String code = "en";

        switch (iso) {
            // Chinese
            case "zh":
                switch (name) {
                    // Chinese - Traditional
                    case "zh-Hant":
                    case "zh-HK":
                    case "zh-MO":
                    case "zh-TW":
                        code = "zh_tw";
                        break;
                    // Mandarin
                    case "zh-cmn":
                        code = "zh_cmn";
                        break;
                    // Wu
                    case "zh-wuu":
                        code = "zh_wuu";
                        break;
                    // Xiang
                    case "zh-hsn":
                        code = "zh_hsn";
                        break;
                    // Cantonese
                    case "zh-yue":
                        code = "zh_yue";
                        break;
                    // Chinese - Simplified
                    default:
                        code = "zh";
                        break;
                }
                break;
            default:
                code = iso;
                break;
        }

        return code;
    }
}
