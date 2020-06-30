package com.thewizrd.simpleweather.controls;

import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.Locale;
import java.util.concurrent.Callable;

public class LocationPanelViewModel {
    private WeatherManager wm;
    private Weather weather;

    private String locationName;
    private String currTemp;
    private String weatherIcon;
    private ImageDataViewModel imageData;
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

    public ImageDataViewModel getImageData() {
        return imageData;
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
        if (weather != null && weather.isValid() && !ObjectsCompat.equals(this.weather, weather)) {
            this.weather = weather;

            imageData = null;

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
                locationData.setLatitude(Double.parseDouble(weather.getLocation().getLatitude()));
                locationData.setLongitude(Double.parseDouble(weather.getLocation().getLongitude()));
                locationData.setTzLong(weather.getLocation().getTzLong());
            }
        }
    }

    public void updateBackground() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                if (weather != null) {
                    ImageDataViewModel imageVM = WeatherUtils.getImageData(weather);

                    if (imageVM != null) {
                        imageData = imageVM;
                    } else {
                        imageData = null;
                    }
                }

                return null;
            }
        });
    }
}
