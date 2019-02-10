package com.thewizrd.simpleweather.controls;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.Locale;

public class LocationPanelViewModel {
    private WeatherManager wm;

    private String locationName;
    private String currTemp;
    private String weatherIcon;
    private String background;
    private LocationData locationData;
    private int locationType = LocationType.SEARCH.getValue();
    private String weatherSource;

    private boolean editMode = false;

    public String getLocationName() {
        return locationName;
    }

    public String getCurrTemp() {
        return currTemp;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public String getBackground() {
        return background;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public String getWeatherSource() {
        return weatherSource;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData = locationData;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public int getLocationType() {
        if (locationData != null)
            return locationData.getLocationType().getValue();
        return locationType;
    }

    public LocationPanelViewModel() {
        wm = WeatherManager.getInstance();
    }

    public LocationPanelViewModel(Weather weather) {
        wm = WeatherManager.getInstance();
        setWeather(weather);
    }

    public void setWeather(Weather weather) {
        // Update background
        background = wm.getWeatherBackgroundURI(weather);

        locationName = weather.getLocation().getName();
        currTemp = (Settings.isFahrenheit() ?
                String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getTempF())) :
                String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getTempC())));
        weatherIcon = weather.getCondition().getIcon();
        weatherSource = weather.getSource();

        if (locationData == null) {
            locationData = new LocationData();
            locationData.setQuery(weather.getQuery());
            locationData.setName(weather.getLocation().getName());
            locationData.setLatitude(Double.valueOf(weather.getLocation().getLatitude()));
            locationData.setLongitude(Double.valueOf(weather.getLocation().getLongitude()));
            locationData.setTzLong(weather.getLocation().getTzLong());
        }
    }
}
