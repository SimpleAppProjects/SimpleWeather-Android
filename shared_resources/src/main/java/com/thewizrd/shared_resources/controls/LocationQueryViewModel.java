package com.thewizrd.shared_resources.controls;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.text.DecimalFormat;
import java.util.Locale;

public class LocationQueryViewModel {
    private String locationName;
    private String locationRegion;
    private String locationCountry;
    private String locationQuery;

    private double locationLat;
    private double locationLong;

    private String locationTZLong;

    private String locationSource;
    private String weatherSource;

    public LocationQueryViewModel() {
        locationName = SimpleLibrary.getInstance().getAppContext().getString(R.string.error_noresults);
        locationCountry = "";
        locationQuery = "";
    }

    public LocationQueryViewModel(LocationData data) {
        locationQuery = data.getQuery();
        locationName = data.getName();
        locationLat = data.getLatitude();
        locationLong = data.getLongitude();
        locationTZLong = data.getTzLong();
        weatherSource = data.getWeatherSource();
        locationSource = data.getLocationSource();
        locationCountry = data.getCountryCode();
    }

    public static LocationQueryViewModel clone(LocationQueryViewModel model) {
        LocationQueryViewModel newModel = new LocationQueryViewModel();

        newModel.locationQuery = model.getLocationQuery();
        newModel.locationName = model.getLocationName();
        newModel.locationLat = model.getLocationLat();
        newModel.locationLong = model.getLocationLong();
        newModel.locationTZLong = model.getLocationTZLong();
        newModel.weatherSource = model.getWeatherSource();
        newModel.locationSource = model.getLocationSource();
        newModel.locationCountry = model.getLocationCountry();

        return newModel;
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static LocationQueryViewModel buildEmptyModel(String weatherSource) {
        LocationQueryViewModel vm = new LocationQueryViewModel();
        vm.locationName = ""; // Reset name
        vm.updateWeatherSource(weatherSource);
        return vm;
    }

    private void updateLocationQuery() {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");

        if (WeatherAPI.HERE.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(locationLat), df.format(locationLong));
        } else if (WeatherAPI.WEATHERUNLOCKED.equals(weatherSource) || WeatherAPI.WEATHERAPI.equals(weatherSource) || WeatherAPI.TOMORROWIO.equals(weatherSource) || WeatherAPI.ACCUWEATHER.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "%s,%s", df.format(locationLat), df.format(locationLong));
        } else {
            locationQuery = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(locationLat), df.format(locationLong));
        }
    }

    public void updateWeatherSource(@WeatherAPI.WeatherProviders @NonNull String API) {
        weatherSource = API;
        updateLocationQuery();
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationRegion() {
        return locationRegion;
    }

    public void setLocationRegion(String locationRegion) {
        this.locationRegion = locationRegion;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationQuery() {
        return locationQuery;
    }

    public void setLocationQuery(String locationQuery) {
        this.locationQuery = locationQuery;
    }

    public double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(double locationLat) {
        this.locationLat = locationLat;
    }

    public double getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(double locationLong) {
        this.locationLong = locationLong;
    }

    public String getLocationTZLong() {
        return locationTZLong;
    }

    public void setLocationTZLong(String locationTZLong) {
        this.locationTZLong = locationTZLong;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public String getWeatherSource() {
        return weatherSource;
    }

    public void setWeatherSource(String weatherSource) {
        this.weatherSource = weatherSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationQueryViewModel that = (LocationQueryViewModel) o;

        if (locationName != null ? !locationName.equals(that.locationName) : that.locationName != null)
            return false;
        if (locationRegion != null ? !locationRegion.equals(that.locationRegion) : that.locationRegion != null)
            return false;
        return locationCountry != null ? locationCountry.equals(that.locationCountry) : that.locationCountry == null;
    }

    @Override
    public int hashCode() {
        int result = locationName != null ? locationName.hashCode() : 0;
        result = 31 * result + (locationRegion != null ? locationRegion.hashCode() : 0);
        result = 31 * result + (locationCountry != null ? locationCountry.hashCode() : 0);
        return result;
    }
}
