package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.here.AdditionalDataItem;

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

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.here.SuggestionsItem location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.here.SuggestionsItem location) {
        String town, region;

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(location.getAddress().getDistrict()))
            town = location.getAddress().getDistrict();
        else
            town = location.getAddress().getCity();

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(location.getAddress().getState()))
            region = location.getAddress().getState();
        else
            region = location.getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(location.getAddress().getCounty())
                && !(location.getAddress().getCounty().equals(region) || location.getAddress().getCounty().equals(town)))
            locationName = String.format("%s, %s, %s", town, location.getAddress().getCounty(), region);
        else
            locationName = String.format("%s, %s", town, region);

        locationCountry = location.getCountryCode();
        locationQuery = location.getLocationId();

        locationLat = -1;
        locationLong = -1;

        locationTZLong = null;
    }

    public LocationQueryViewModel(com.thewizrd.shared_resources.weatherdata.here.ResultItem location) {
        setLocation(location);
    }

    public void setLocation(com.thewizrd.shared_resources.weatherdata.here.ResultItem location) {
        String country = null, region = null, town = null;

        if (location.getLocation().getAddress().getAdditionalData() != null) {
            for (AdditionalDataItem item : location.getLocation().getAddress().getAdditionalData()) {
                if ("Country2".equals(item.getKey()))
                    country = item.getValue();
                else if ("StateName".equals(item.getKey()))
                    region = item.getValue();

                if (country != null && region != null)
                    break;
            }
        }

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(location.getLocation().getAddress().getDistrict()))
            town = location.getLocation().getAddress().getDistrict();
        else
            town = location.getLocation().getAddress().getCity();

        if (StringUtils.isNullOrEmpty(region))
            region = location.getLocation().getAddress().getState();

        if (StringUtils.isNullOrEmpty(region))
            region = location.getLocation().getAddress().getCounty();

        if (StringUtils.isNullOrEmpty(country))
            country = location.getLocation().getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(location.getLocation().getAddress().getCounty())
                && !(location.getLocation().getAddress().getCounty().equals(region) || location.getLocation().getAddress().getCounty().equals(town)))
            locationName = String.format("%s, %s, %s", town, location.getLocation().getAddress().getCounty(), region);
        else
            locationName = String.format("%s, %s", town, region);
        locationCountry = country;
        locationQuery = String.format(Locale.ROOT, "latitude=%f&longitude=%f",
                location.getLocation().getDisplayPosition().getLatitude(), location.getLocation().getDisplayPosition().getLongitude());

        locationLat = location.getLocation().getDisplayPosition().getLatitude();
        locationLong = location.getLocation().getDisplayPosition().getLongitude();

        locationTZLong = location.getLocation().getAdminInfo().getTimeZone().getId();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationQueryViewModel that = (LocationQueryViewModel) o;

        if (locationName != null ? !locationName.equals(that.locationName) : that.locationName != null)
            return false;
        //if (locationCountry != null ? !locationCountry.equals(that.locationCountry) : that.locationCountry != null)
        //    return false;
        return locationCountry != null ? locationCountry.equals(that.locationCountry) : that.locationCountry == null;
        //return locationQuery != null ? locationQuery.equals(that.locationQuery) : that.locationQuery == null;
    }

    @Override
    public int hashCode() {
        int result = locationName != null ? locationName.hashCode() : 0;
        result = 31 * result + (locationCountry != null ? locationCountry.hashCode() : 0);
        //result = 31 * result + (locationQuery != null ? locationQuery.hashCode() : 0);
        return result;
    }
}
