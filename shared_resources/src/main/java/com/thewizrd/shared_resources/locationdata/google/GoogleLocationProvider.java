package com.thewizrd.shared_resources.locationdata.google;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class GoogleLocationProvider extends LocationProviderImpl {
    private AutocompleteSessionToken autocompleteToken;
    private static final List<Place.Field> BASIC_PLACE_FIELDS = Arrays.asList(Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.TYPES);

    @WeatherAPI.LocationAPIs
    @Override
    public String getLocationAPI() {
        return WeatherAPI.GOOGLE;
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
    public boolean needsLocationFromID() {
        return true;
    }

    @Override
    public boolean needsLocationFromName() {
        return true;
    }

    private void refreshToken() {
        if (autocompleteToken == null) {
            autocompleteToken = AutocompleteSessionToken.newInstance();
        }
    }

    private PlacesClient getPlacesClient() {
        Context ctx = SimpleLibrary.getInstance().getAppContext();
        Places.initialize(ctx, getAPIKey(), LocaleUtils.getLocale());
        return Places.createClient(ctx);
    }

    @Override
    public Collection<LocationQueryViewModel> getLocations(String ac_query, String weatherAPI) throws WeatherException {
        if (!Geocoder.isPresent()) {
            throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
        }

        Collection<LocationQueryViewModel> locations = null;
        WeatherException wEx = null;

        try {
            // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
            // and once again when the user makes a selection (for example when calling fetchPlace()).
            refreshToken();

            // Use the builder to create a FindAutocompletePredictionsRequest.
            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                    .setTypeFilter(TypeFilter.CITIES)
                    .setSessionToken(autocompleteToken)
                    .setQuery(ac_query)
                    .build();

            PlacesClient placesClient = getPlacesClient();

            FindAutocompletePredictionsResponse response =
                    AsyncTask.await(placesClient.findAutocompletePredictions(request));

            locations = new HashSet<>();

            for (AutocompletePrediction result : response.getAutocompletePredictions()) {
                locations.add(new LocationQueryViewModel(result, weatherAPI));
            }
        } catch (Throwable ex) {
            if (ex instanceof ExecutionException) {
                ex = ex.getCause();
            }

            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            } else if (ex instanceof IllegalArgumentException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
            } else if (ex instanceof ApiException) {
                switch (((ApiException) ex).getStatusCode()) {
                    case CommonStatusCodes.NETWORK_ERROR:
                    case CommonStatusCodes.RECONNECTION_TIMED_OUT:
                    case CommonStatusCodes.RECONNECTION_TIMED_OUT_DURING_UPDATE:
                    case CommonStatusCodes.API_NOT_CONNECTED:
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
                        break;
                    case CommonStatusCodes.ERROR:
                    case CommonStatusCodes.INTERNAL_ERROR:
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
                        break;
                }
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location");
        }

        if (wEx != null)
            throw wEx;

        if (locations == null || locations.size() == 0) {
            locations = Collections.singletonList(new LocationQueryViewModel());
        }

        return locations;
    }

    public LocationQueryViewModel getLocationFromID(@NonNull LocationQueryViewModel model) throws WeatherException {
        if (!Geocoder.isPresent()) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
        }

        LocationQueryViewModel location;
        FetchPlaceResponse response = null;
        WeatherException wEx = null;

        try {
            FetchPlaceRequest request = FetchPlaceRequest.builder(model.getLocationQuery(), BASIC_PLACE_FIELDS)
                    .setSessionToken(autocompleteToken)
                    .build();

            PlacesClient placesClient = getPlacesClient();
            response = AsyncTask.await(placesClient.fetchPlace(request));

            autocompleteToken = null;
        } catch (Throwable ex) {
            if (ex instanceof ExecutionException) {
                ex = ex.getCause();
            }

            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            } else if (ex instanceof IllegalArgumentException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
            } else if (ex instanceof ApiException) {
                switch (((ApiException) ex).getStatusCode()) {
                    case CommonStatusCodes.NETWORK_ERROR:
                    case CommonStatusCodes.RECONNECTION_TIMED_OUT:
                    case CommonStatusCodes.RECONNECTION_TIMED_OUT_DURING_UPDATE:
                    case CommonStatusCodes.API_NOT_CONNECTED:
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
                        break;
                    case CommonStatusCodes.ERROR:
                    case CommonStatusCodes.INTERNAL_ERROR:
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
                        break;
                }
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location");
        }

        if (wEx != null)
            throw wEx;

        if (response != null)
            location = new LocationQueryViewModel(response, model.getWeatherSource());
        else
            location = new LocationQueryViewModel();

        return location;
    }

    public LocationQueryViewModel getLocationFromName(@NonNull LocationQueryViewModel model) throws WeatherException {
        if (!Geocoder.isPresent()) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
        }

        LocationQueryViewModel location;
        Address result;
        WeatherException wEx = null;

        try {
            Geocoder geocoder = new Geocoder(SimpleLibrary.getInstance().getAppContext(), LocaleUtils.getLocale());
            List<Address> addresses = geocoder.getFromLocationName(model.getLocationName(), 1);

            result = addresses.get(0);
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            } else if (ex instanceof IllegalArgumentException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location");
        }

        if (wEx != null)
            throw wEx;

        if (result != null)
            location = new LocationQueryViewModel(result, model.getWeatherSource());
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public LocationQueryViewModel getLocation(@NonNull WeatherUtils.Coordinate coordinate, String weatherAPI) throws WeatherException {
        if (!Geocoder.isPresent()) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
        }

        LocationQueryViewModel location;
        Address result;
        WeatherException wEx = null;

        try {
            Geocoder geocoder = new Geocoder(SimpleLibrary.getInstance().getAppContext(), LocaleUtils.getLocale());
            List<Address> addresses = geocoder.getFromLocation(coordinate.getLatitude(), coordinate.getLongitude(), 1);

            result = addresses.get(0);
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            } else if (ex instanceof IllegalArgumentException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
            }
            Logger.writeLine(Log.ERROR, ex, "GoogleLocationProvider: error getting location");
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
        return Keys.getGPlacesKey();
    }
}
