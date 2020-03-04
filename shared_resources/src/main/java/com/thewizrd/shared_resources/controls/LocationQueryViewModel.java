package com.thewizrd.shared_resources.controls;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.here.AdditionalDataItem;
import com.thewizrd.shared_resources.locationdata.here.ResultItem;
import com.thewizrd.shared_resources.locationdata.here.SuggestionsItem;
import com.thewizrd.shared_resources.locationdata.locationiq.AutoCompleteQuery;
import com.thewizrd.shared_resources.locationdata.locationiq.GeoLocation;
import com.thewizrd.shared_resources.locationdata.weatherunderground.AC_RESULTS;
import com.thewizrd.shared_resources.locationdata.weatherunderground.Location;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.util.Locale;

public class LocationQueryViewModel {
    private String locationName;
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

    public LocationQueryViewModel(AC_RESULTS location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    public void setLocation(AC_RESULTS location, String weatherAPI) {
        if (location == null)
            return;

        locationName = location.getName();
        locationCountry = location.getC();

        locationLat = Double.parseDouble(location.getLat());
        locationLong = Double.parseDouble(location.getLon());

        locationTZLong = location.getTz();

        locationSource = WeatherAPI.WEATHERUNDERGROUND;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    public LocationQueryViewModel(Location location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    public void setLocation(Location location, String weatherAPI) {
        if (location == null)
            return;

        locationName = String.format("%s, %s", location.getCity(), location.getState());
        locationCountry = location.getCountry();

        locationLat = Double.parseDouble(location.getLat());
        locationLong = Double.parseDouble(location.getLon());

        locationTZLong = location.getTzUnix();

        locationSource = WeatherAPI.WEATHERUNDERGROUND;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    public LocationQueryViewModel(SuggestionsItem location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    public void setLocation(SuggestionsItem location, String weatherAPI) {
        if (location == null || location.getAddress() == null)
            return;

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

        locationSource = WeatherAPI.HERE;
        weatherSource = weatherAPI;
    }

    public LocationQueryViewModel(ResultItem location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    public void setLocation(ResultItem location, String weatherAPI) {
        if (location == null || location.getLocation() == null || location.getLocation().getAddress() == null)
            return;

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

        locationLat = location.getLocation().getDisplayPosition().getLatitude();
        locationLong = location.getLocation().getDisplayPosition().getLongitude();

        locationTZLong = location.getLocation().getAdminInfo().getTimeZone().getId();

        locationSource = WeatherAPI.HERE;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    public LocationQueryViewModel(AutoCompleteQuery result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    private void setLocation(AutoCompleteQuery result, String weatherAPI) {
        if (result == null || result.getAddress() == null)
            return;

        String town, region;

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getNeighbourhood()))
            town = result.getAddress().getNeighbourhood();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getHamlet()))
            town = result.getAddress().getHamlet();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getSuburb()))
            town = result.getAddress().getSuburb();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getVillage()))
            town = result.getAddress().getVillage();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getTown()))
            town = result.getAddress().getTown();
        else if (!StringUtils.isNullOrWhitespace(result.getAddress().getCity()))
            town = result.getAddress().getCity();
        else
            town = result.getAddress().getName();

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getRegion()))
            region = result.getAddress().getRegion();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getCounty()))
            region = result.getAddress().getCounty();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getStateDistrict()))
            region = result.getAddress().getStateDistrict();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getState()))
            region = result.getAddress().getState();
        else
            region = result.getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(result.getAddress().getName()) && !(result.getAddress().getName().equals(town)))
            locationName = String.format("%s, %s, %s", result.getAddress().getName(), town, region);
        else
            locationName = String.format("%s, %s", town, region);

        if (!StringUtils.isNullOrWhitespace(result.getAddress().getCountryCode()))
            locationCountry = result.getAddress().getCountryCode().toUpperCase(Locale.ROOT);
        else
            locationCountry = result.getAddress().getCountry();

        locationLat = Double.parseDouble(result.getLat());
        locationLong = Double.parseDouble(result.getLon());

        locationTZLong = null;

        locationSource = WeatherAPI.LOCATIONIQ;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    public LocationQueryViewModel(GeoLocation result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    private void setLocation(GeoLocation result, String weatherAPI) {
        if (result == null || result.getAddress() == null)
            return;

        String town, region;

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getNeighbourhood()))
            town = result.getAddress().getNeighbourhood();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getHamlet()))
            town = result.getAddress().getHamlet();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getSuburb()))
            town = result.getAddress().getSuburb();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getVillage()))
            town = result.getAddress().getVillage();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getTown()))
            town = result.getAddress().getTown();
        else if (!StringUtils.isNullOrWhitespace(result.getAddress().getCity()))
            town = result.getAddress().getCity();
        else
            town = result.getAddress().getName();

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getRegion()))
            region = result.getAddress().getRegion();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getCounty()))
            region = result.getAddress().getCounty();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getStateDistrict()))
            region = result.getAddress().getStateDistrict();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getState()))
            region = result.getAddress().getState();
        else
            region = result.getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(result.getAddress().getName()) && !(result.getAddress().getName().equals(town)))
            locationName = String.format("%s, %s, %s", result.getAddress().getName(), town, region);
        else
            locationName = String.format("%s, %s", town, region);

        if (!StringUtils.isNullOrWhitespace(result.getAddress().getCountryCode()))
            locationCountry = result.getAddress().getCountryCode().toUpperCase(Locale.ROOT);
        else
            locationCountry = result.getAddress().getCountry();

        locationLat = Double.parseDouble(result.getLat());
        locationLong = Double.parseDouble(result.getLon());

        locationTZLong = null;

        locationSource = WeatherAPI.LOCATIONIQ;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    private void updateLocationQuery() {
        if (WeatherAPI.WEATHERUNDERGROUND.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "/q/%s,%s", Double.toString(locationLat), Double.toString(locationLong));
        } else if (WeatherAPI.HERE.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "latitude=%s&longitude=%s", Double.toString(locationLat), Double.toString(locationLong));
        } else if (WeatherAPI.NWS.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "%s,%s", Double.toString(locationLat), Double.toString(locationLong));
        } else {
            locationQuery = String.format(Locale.ROOT, "lat=%s&lon=%s", Double.toString(locationLat), Double.toString(locationLong));
        }
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
