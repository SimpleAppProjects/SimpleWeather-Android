package com.thewizrd.simpleweather.controls;

import androidx.annotation.WorkerThread;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

public class LocationPanelViewModel {
    private WeatherManager wm;
    private Weather weather;

    private String locationName;
    private String currTemp;
    private String currWeather;
    private String weatherIcon;
    private String hiTemp;
    private String loTemp;
    private boolean showHiLo;
    private String pop;
    private String popIcon;
    private int windDir;
    private String windSpeed;
    private ImageDataViewModel imageData;
    private LocationData locationData;
    private int locationType = LocationType.SEARCH.getValue();
    private String weatherSource;

    private boolean editMode = false;
    private boolean checked = false;

    public String getLocationName() {
        return locationName;
    }

    public String getCurrTemp() {
        return currTemp;
    }

    public String getCurrWeather() {
        return currWeather;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public String getHiTemp() {
        return hiTemp;
    }

    public String getLoTemp() {
        return loTemp;
    }

    public boolean isShowHiLo() {
        return showHiLo;
    }

    public String getPop() {
        return pop;
    }

    public String getPopIcon() {
        return popIcon;
    }

    public int getWindDir() {
        return windDir;
    }

    public String getWindSpeed() {
        return windSpeed;
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

    public boolean isChecked() {
        return checked;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData = locationData;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
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

            if (weather.getCondition().getTempF() != null && !ObjectsCompat.equals(weather.getCondition().getTempF(), weather.getCondition().getTempC())) {
                int temp = Settings.isFahrenheit() ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC());
                String unitTemp = Settings.isFahrenheit() ? WeatherIcons.FAHRENHEIT : WeatherIcons.CELSIUS;

                currTemp = String.format(LocaleUtils.getLocale(), "%d%s", temp, unitTemp);
            } else {
                currTemp = "--";
            }

            currWeather = weather.getCondition().getWeather();

            if (weather.getCondition().getHighF() != null && !ObjectsCompat.equals(weather.getCondition().getHighF(), weather.getCondition().getHighC())) {
                int temp = Settings.isFahrenheit() ? Math.round(weather.getCondition().getHighF()) : Math.round(weather.getCondition().getHighC());
                hiTemp = String.format(LocaleUtils.getLocale(), "%d°", temp);
            } else {
                hiTemp = "--";
            }

            if (weather.getCondition().getLowF() != null && !ObjectsCompat.equals(weather.getCondition().getLowF(), weather.getCondition().getLowC())) {
                int temp = Settings.isFahrenheit() ? Math.round(weather.getCondition().getLowF()) : Math.round(weather.getCondition().getLowC());
                loTemp = String.format(LocaleUtils.getLocale(), "%d°", temp);
            } else {
                loTemp = "--";
            }

            showHiLo = !ObjectsCompat.equals(hiTemp, loTemp);

            if (weather.getCondition().getWindMph() != null && weather.getCondition().getWindMph() >= 0 &&
                    weather.getCondition().getWindDegrees() != null && weather.getCondition().getWindDegrees() >= 0) {
                int speedVal = Settings.isFahrenheit() ? Math.round(weather.getCondition().getWindMph()) : Math.round(weather.getCondition().getWindKph());
                String speedUnit = WeatherUtils.getSpeedUnit();

                windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
                windDir = weather.getCondition().getWindDegrees() + 180;
            } else {
                windSpeed = "--";
                windDir = 0;
            }

            if (weather.getPrecipitation() != null) {
                if (weather.getPrecipitation().getPop() != null) {
                    pop = weather.getPrecipitation().getPop() + "%";
                    popIcon = WeatherIcons.UMBRELLA;
                } else if (weather.getPrecipitation().getCloudiness() != null) {
                    pop = weather.getPrecipitation().getCloudiness() + "%";
                    popIcon = WeatherIcons.CLOUDY;
                }
            }

            weatherIcon = weather.getCondition().getIcon();
            weatherSource = weather.getSource();

            if (locationData == null) {
                locationData = new LocationData();
                locationData.setQuery(weather.getQuery());
                locationData.setName(weather.getLocation().getName());
                locationData.setLatitude(NumberUtils.getValueOrDefault(weather.getLocation().getLatitude(), 0));
                locationData.setLongitude(NumberUtils.getValueOrDefault(weather.getLocation().getLongitude(), 0));
                locationData.setTzLong(weather.getLocation().getTzLong());
            }
        }
    }

    @WorkerThread
    public void updateBackground() {
        if (weather != null) {
            imageData = WeatherUtils.getImageData(weather);
        }
    }
}
