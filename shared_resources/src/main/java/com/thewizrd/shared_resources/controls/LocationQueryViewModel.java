package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.util.Locale;

public class LocationQueryViewModel {
    private String locationName;
    private String locationCountry;
    private String locationQuery;

    private double locationLat;
    private double locationLong;

    private String locationTZLong;

    public LocationQueryViewModel() {
        locationName = SimpleLibrary.getInstance().getAppContext().getString(R.string.error_noresults);
        locationCountry = "";
        locationQuery = "";
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.weatherunderground.AC_RESULTS location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.weatherunderground.AC_RESULTS location) {
        locationName = location.getName();
        locationCountry = location.getC();
        locationQuery = location.getL();

        locationLat = Double.valueOf(location.getLat());
        locationLong = Double.valueOf(location.getLon());

        locationTZLong = location.getTz();
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.weatherunderground.Location location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.weatherunderground.Location location) {
        locationName = String.format("%s, %s", location.getCity(), location.getState());
        locationCountry = location.getCountry();
        locationQuery = location.getQuery();

        locationLat = Double.valueOf(location.getLat());
        locationLong = Double.valueOf(location.getLon());

        locationTZLong = location.getTzUnix();
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.weatheryahoo.AutoCompleteQuery.Place location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.weatheryahoo.AutoCompleteQuery.Place location) {
        String town, region;

        // If location type is ZipCode append it to location name
        if ((location.getPlaceTypeName().getTextValue().equals("Zip Code")
                || location.getPlaceTypeName().getTextValue().equals("Postal Code"))) {
            town = location.getName();

            if (location.getLocality2() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality2().getTextValue())) {
                town += " - " + location.getLocality2().getTextValue();
            } else {
                if (location.getLocality1() != null
                        && !StringUtils.isNullOrEmpty(location.getLocality1().getTextValue()))
                    town += " - " + location.getLocality1().getTextValue();
            }
        } else {
            if (location.getLocality2() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality2().getTextValue()))
                town = location.getLocality2().getTextValue();
            else if (location.getLocality1() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality1().getTextValue()))
                town = location.getLocality1().getTextValue();
            else
                town = location.getName();
        }

        // Try to get region name or fallback to country name
        if (location.getAdmin1() != null
                && !StringUtils.isNullOrEmpty(location.getAdmin1().getTextValue()))
            region = location.getAdmin1().getTextValue();
        else if (location.getAdmin2() != null
                && !StringUtils.isNullOrEmpty(location.getAdmin2().getTextValue()))
            region = location.getAdmin2().getTextValue();
        else
            region = location.getCountry().getTextValue();

        locationName = String.format("%s, %s", town, region);
        locationCountry = location.getCountry().getCode();
        locationQuery = location.getWoeid();

        locationLat = location.getCentroid().getLatitude().doubleValue();
        locationLong = location.getCentroid().getLongitude().doubleValue();

        locationTZLong = location.getTimezone().getTextValue();
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.openweather.AC_RESULTS location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.openweather.AC_RESULTS location) {
        locationName = location.getName();
        locationCountry = location.getC();
        locationQuery = String.format("lat=%s&lon=%s", location.getLat(), location.getLon());

        locationLat = Double.valueOf(location.getLat());
        locationLong = Double.valueOf(location.getLon());

        locationTZLong = location.getTz();
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.openweather.Location location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.openweather.Location location) {
        locationName = String.format("%s, %s", location.getCity(), location.getState());
        locationCountry = location.getCountry();
        locationQuery = String.format("lat=%s&lon=%s", location.getLat(), location.getLon());

        locationLat = Double.valueOf(location.getLat());
        locationLong = Double.valueOf(location.getLon());

        locationTZLong = location.getTzUnix();
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.here.AutoCompleteQuery.Place location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.here.AutoCompleteQuery.Place location) {
        String town, region;

        // If location type is ZipCode append it to location name
        if ((location.getPlaceTypeName().getTextValue().equals("Zip Code")
                || location.getPlaceTypeName().getTextValue().equals("Postal Code"))) {
            town = location.getName();

            if (location.getLocality2() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality2().getTextValue())) {
                town += " - " + location.getLocality2().getTextValue();
            } else {
                if (location.getLocality1() != null
                        && !StringUtils.isNullOrEmpty(location.getLocality1().getTextValue()))
                    town += " - " + location.getLocality1().getTextValue();
            }
        } else {
            if (location.getLocality2() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality2().getTextValue()))
                town = location.getLocality2().getTextValue();
            else if (location.getLocality1() != null
                    && !StringUtils.isNullOrEmpty(location.getLocality1().getTextValue()))
                town = location.getLocality1().getTextValue();
            else
                town = location.getName();
        }

        // Try to get region name or fallback to country name
        if (location.getAdmin1() != null
                && !StringUtils.isNullOrEmpty(location.getAdmin1().getTextValue()))
            region = location.getAdmin1().getTextValue();
        else if (location.getAdmin2() != null
                && !StringUtils.isNullOrEmpty(location.getAdmin2().getTextValue()))
            region = location.getAdmin2().getTextValue();
        else
            region = location.getCountry().getTextValue();

        locationName = String.format("%s, %s", town, region);
        locationCountry = location.getCountry().getCode();
        locationQuery = String.format(Locale.ROOT, "latitude=%f&longitude=%f", location.getCentroid().getLatitude(), location.getCentroid().getLongitude());

        locationLat = location.getCentroid().getLatitude().doubleValue();
        locationLong = location.getCentroid().getLongitude().doubleValue();

        locationTZLong = location.getTimezone().getTextValue();
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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
}
