package com.thewizrd.shared_resources.weatherdata;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.skedgo.converter.TimezoneMapper;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.nws.NWSAlertProvider;

import java.util.Collection;
import java.util.List;

public abstract class WeatherProviderImpl implements WeatherProviderImplInterface {
    protected Handler mMainHandler;
    protected LocationProviderImpl locationProvider;

    public WeatherProviderImpl() {
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    // Variables
    public abstract String getWeatherAPI();

    public abstract boolean isKeyRequired();

    public abstract boolean supportsWeatherLocale();

    public abstract boolean supportsAlerts();

    public abstract boolean needsExternalAlertData();

    // Methods
    // AutoCompleteQuery
    public final Collection<LocationQueryViewModel> getLocations(String ac_query) {
        return locationProvider.getLocations(ac_query, getWeatherAPI());
    }

    // GeopositionQuery
    public final LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate) {
        return locationProvider.getLocation(coordinate, getWeatherAPI());
    }

    public final LocationQueryViewModel getLocation(String query) {
        return locationProvider.getLocation(query, getWeatherAPI());
    }

    // Weather
    public abstract Weather getWeather(String location_query) throws WeatherException;

    @Override
    public Weather getWeather(LocationData location) throws WeatherException {
        if (location == null || location.getQuery() == null)
            throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);

        Weather weather = getWeather(location.getQuery());

        if (supportsAlerts() && needsExternalAlertData())
            weather.setWeatherAlerts(getAlerts(location));

        if (StringUtils.isNullOrWhitespace(location.getTzLong())) {
            if (!StringUtils.isNullOrWhitespace(weather.getLocation().getTzLong())) {
                location.setTzLong(weather.getLocation().getTzLong());
            } else if (location.getLongitude() != 0 && location.getLatitude() != 0) {
                String tzId = TimezoneMapper.latLngToTimezoneString(location.getLatitude(), location.getLongitude());
                if (!"unknown".equals(tzId))
                    location.setTzLong(tzId);
            }

            // Update DB here or somewhere else
            if (SimpleLibrary.getInstance().getApp().isPhone()) {
                Settings.updateLocation(location);
            } else {
                Settings.saveHomeData(location);
            }
        }

        if (StringUtils.isNullOrWhitespace(weather.getLocation().getTzLong()))
            weather.getLocation().setTzLong(location.getTzLong());

        if (StringUtils.isNullOrWhitespace(weather.getLocation().getName()))
            weather.getLocation().setName(location.getName());

        weather.getLocation().setLatitude(Double.toString(location.getLatitude()));
        weather.getLocation().setLongitude(Double.toString(location.getLongitude()));
        weather.getLocation().setTzShort(location.getTzShort());
        weather.getLocation().setTzOffset(location.getTzOffset());

        return weather;
    }

    // Alerts
    @Override
    public List<WeatherAlert> getAlerts(LocationData location) {
        if ("US".equals(location.getCountryCode()))
            return new NWSAlertProvider().getAlerts(location);
        else
            return null;
    }

    // KeyCheck
    public abstract boolean isKeyValid(String key);

    public abstract String getAPIKey();

    // Utils Methods
    @Override
    public final void updateLocationData(LocationData location) {
        locationProvider.updateLocationData(location, getWeatherAPI());
    }

    public abstract String updateLocationQuery(Weather weather);

    public abstract String updateLocationQuery(LocationData location);

    @Override
    public String localeToLangCode(String iso, String name) {
        return "EN";
    }

    public abstract String getWeatherIcon(String icon);

    // Used in some providers for hourly forecast
    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        return getWeatherIcon(icon);
    }

    @Override
    public boolean isNight(Weather weather) {
        boolean isNight = false;

        String icon = weather.getCondition().getIcon();

        switch (icon) {
            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
                isNight = true;
                break;
        }

        return isNight;
    }

    @Override
    public int getWeatherBackgroundColor(Weather weather) {
        String rgbHex = null;
        String icon = weather.getCondition().getIcon();

        // Apply background based on weather condition
        switch (icon) {
            // Rain/Snow/Sleet/Hail/Storms
            case WeatherIcons.DAY_HAIL:
            case WeatherIcons.DAY_LIGHTNING:
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.DAY_RAIN_WIND:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.DAY_SPRINKLE:
            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.DAY_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.HAIL:
            case WeatherIcons.RAIN:
            case WeatherIcons.RAIN_MIX:
            case WeatherIcons.RAIN_WIND:
            case WeatherIcons.SHOWERS:
            case WeatherIcons.SLEET:
            case WeatherIcons.SNOW:
            case WeatherIcons.SPRINKLE:
            case WeatherIcons.STORM_SHOWERS:
            case WeatherIcons.THUNDERSTORM:
            case WeatherIcons.SNOW_WIND:
            case WeatherIcons.LIGHTNING:
                // lighter than night color + cloudiness
                rgbHex = "#354374";
                break;
            // Dust
            case WeatherIcons.DUST:
                // Foggy / Haze
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.DAY_HAZE:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.FOG:
                // add haziness
                rgbHex = "#8FA3C4";
                break;
            // Night
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                // Night background
                rgbHex = "#1A244A";
                break;
            // Mostly/Partly Cloudy
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY_HIGH:
            case WeatherIcons.DAY_SUNNY_OVERCAST:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.CLOUD:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.CLOUDY_GUSTS:
            case WeatherIcons.CLOUDY_WINDY:
                if (isNight(weather)) {
                    // Add night background plus cloudiness
                    rgbHex = "#102543";
                } else {
                    // add day bg + cloudiness
                    rgbHex = "#7794C4";
                }
                break;
            case WeatherIcons.NA:
            default:
                // Set background based using sunset/rise times
                if (isNight(weather)) {
                    // Night background
                    rgbHex = "#1A244A";
                } else {
                    // set day bg
                    rgbHex = "#4874BF";
                }
                break;
        }

        // Just in case
        if (StringUtils.isNullOrWhitespace(rgbHex)) {
            // Set background based using sunset/rise times
            if (isNight(weather)) {
                // Night background
                rgbHex = "#1A244A";
            } else {
                // set day bg
                rgbHex = "#4874BF";
            }
        }

        return Color.parseColor(rgbHex);
    }

    @Override
    public String getWeatherBackgroundURI(Weather weather) {
        String icon = weather.getCondition().getIcon();
        String file = null;

        // Apply background based on weather condition
        switch (icon) {
            // Rain
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.DAY_RAIN_WIND:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.DAY_SPRINKLE:
            case WeatherIcons.HAIL:
            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.RAIN:
            case WeatherIcons.RAIN_MIX:
            case WeatherIcons.RAIN_WIND:
            case WeatherIcons.SHOWERS:
            case WeatherIcons.SLEET:
            case WeatherIcons.SPRINKLE:
                file = "file:///android_asset/backgrounds/RainySky.jpg";
                break;
            // Tornado / Hurricane / Thunderstorm / Tropical Storm
            case WeatherIcons.DAY_LIGHTNING:
            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.DAY_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.HURRICANE:
            case WeatherIcons.LIGHTNING:
            case WeatherIcons.STORM_SHOWERS:
            case WeatherIcons.THUNDERSTORM:
            case WeatherIcons.TORNADO:
                file = "file:///android_asset/backgrounds/StormySky.jpg";
                break;
            // Dust
            case WeatherIcons.DUST:
            case WeatherIcons.SANDSTORM:
                file = "file:///android_asset/backgrounds/Dust.jpg";
                break;
            // Foggy / Haze
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.DAY_HAZE:
            case WeatherIcons.FOG:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.SMOG:
            case WeatherIcons.SMOKE:
                file = "file:///android_asset/backgrounds/FoggySky.jpg";
                break;
            // Snow / Snow Showers/Storm
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
            case WeatherIcons.SNOW:
            case WeatherIcons.SNOW_WIND:
                file = "file:///android_asset/backgrounds/Snow.jpg";
                break;
            /* Ambigious weather conditions */
            // (Mostly) Cloudy
            case WeatherIcons.CLOUD:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.CLOUDY_GUSTS:
            case WeatherIcons.CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.DAY_CLOUDY_HIGH:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                if (isNight(weather))
                    file = "file:///android_asset/backgrounds/MostlyCloudy-Night.jpg";
                else
                    file = "file:///android_asset/backgrounds/MostlyCloudy-Day.jpg";
                break;
            // Partly Cloudy
            case WeatherIcons.DAY_SUNNY_OVERCAST:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                if (isNight(weather))
                    file = "file:///android_asset/backgrounds/PartlyCloudy-Night.jpg";
                else
                    file = "file:///android_asset/backgrounds/PartlyCloudy-Day.jpg";
                break;
            case WeatherIcons.DAY_SUNNY:
            case WeatherIcons.NA:
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.SNOWFLAKE_COLD:
            case WeatherIcons.DAY_HOT:
            case WeatherIcons.WINDY:
            case WeatherIcons.STRONG_WIND:
            default:
                // Set background based using sunset/rise times
                if (isNight(weather))
                    file = "file:///android_asset/backgrounds/NightSky.jpg";
                else
                    file = "file:///android_asset/backgrounds/DaySky.jpg";
                break;
        }

        // Just in case
        if (StringUtils.isNullOrWhitespace(file)) {
            // Set background based using sunset/rise times
            if (isNight(weather))
                file = "file:///android_asset/backgrounds/NightSky.jpg";
            else
                file = "file:///android_asset/backgrounds/DaySky.jpg";
        }

        return file;
    }

    @Override
    public int getWeatherIconResource(String icon) {
        int weatherIcon = -1;

        switch (icon) {
            case WeatherIcons.DAY_SUNNY:
                weatherIcon = R.drawable.day_sunny;
                break;
            case WeatherIcons.DAY_CLOUDY:
                weatherIcon = R.drawable.day_cloudy;
                break;
            case WeatherIcons.DAY_CLOUDY_GUSTS:
                weatherIcon = R.drawable.day_cloudy_gusts;
                break;
            case WeatherIcons.DAY_CLOUDY_WINDY:
                weatherIcon = R.drawable.day_cloudy_windy;
                break;
            case WeatherIcons.DAY_FOG:
                weatherIcon = R.drawable.day_fog;
                break;
            case WeatherIcons.DAY_HAIL:
                weatherIcon = R.drawable.day_hail;
                break;
            case WeatherIcons.DAY_HAZE:
                weatherIcon = R.drawable.day_haze;
                break;
            case WeatherIcons.DAY_LIGHTNING:
                weatherIcon = R.drawable.day_lightning;
                break;
            case WeatherIcons.DAY_RAIN:
                weatherIcon = R.drawable.day_rain;
                break;
            case WeatherIcons.DAY_RAIN_MIX:
                weatherIcon = R.drawable.day_rain_mix;
                break;
            case WeatherIcons.DAY_RAIN_WIND:
                weatherIcon = R.drawable.day_rain_wind;
                break;
            case WeatherIcons.DAY_SHOWERS:
                weatherIcon = R.drawable.day_showers;
                break;
            case WeatherIcons.DAY_SLEET:
                weatherIcon = R.drawable.day_sleet;
                break;
            case WeatherIcons.DAY_SLEET_STORM:
                weatherIcon = R.drawable.day_sleet_storm;
                break;
            case WeatherIcons.DAY_SNOW:
                weatherIcon = R.drawable.day_snow;
                break;
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.day_snow_thunderstorm;
                break;
            case WeatherIcons.DAY_SNOW_WIND:
                weatherIcon = R.drawable.day_snow_wind;
                break;
            case WeatherIcons.DAY_SPRINKLE:
                weatherIcon = R.drawable.day_sprinkle;
                break;
            case WeatherIcons.DAY_STORM_SHOWERS:
                weatherIcon = R.drawable.day_storm_showers;
                break;
            case WeatherIcons.DAY_SUNNY_OVERCAST:
                weatherIcon = R.drawable.day_sunny_overcast;
                break;
            case WeatherIcons.DAY_THUNDERSTORM:
                weatherIcon = R.drawable.day_thunderstorm;
                break;
            case WeatherIcons.DAY_WINDY:
                weatherIcon = R.drawable.day_windy;
                break;
            case WeatherIcons.DAY_HOT:
                weatherIcon = R.drawable.hot;
                break;
            case WeatherIcons.DAY_CLOUDY_HIGH:
                weatherIcon = R.drawable.day_cloudy_high;
                break;
            case WeatherIcons.DAY_LIGHT_WIND:
                weatherIcon = R.drawable.day_light_wind;
                break;
            case WeatherIcons.NIGHT_CLEAR:
                weatherIcon = R.drawable.night_clear;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY:
                weatherIcon = R.drawable.night_alt_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
                weatherIcon = R.drawable.night_alt_cloudy_gusts;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                weatherIcon = R.drawable.night_alt_cloudy_windy;
                break;
            case WeatherIcons.NIGHT_ALT_HAIL:
                weatherIcon = R.drawable.night_alt_hail;
                break;
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
                weatherIcon = R.drawable.night_alt_lightning;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN:
                weatherIcon = R.drawable.night_alt_rain;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
                weatherIcon = R.drawable.night_alt_rain_mix;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
                weatherIcon = R.drawable.night_alt_rain_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SHOWERS:
                weatherIcon = R.drawable.night_alt_showers;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET:
                weatherIcon = R.drawable.night_alt_sleet;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
                weatherIcon = R.drawable.night_alt_sleet_storm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW:
                weatherIcon = R.drawable.night_alt_snow;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.night_alt_snow_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                weatherIcon = R.drawable.night_alt_snow_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
                weatherIcon = R.drawable.night_alt_sprinkle;
                break;
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
                weatherIcon = R.drawable.night_alt_storm_showers;
                break;
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
                weatherIcon = R.drawable.night_alt_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                weatherIcon = R.drawable.night_alt_partly_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
                weatherIcon = R.drawable.night_alt_cloudy_high;
                break;
            case WeatherIcons.NIGHT_FOG:
                weatherIcon = R.drawable.night_fog;
                break;
            case WeatherIcons.CLOUD:
                weatherIcon = R.drawable.cloud;
                break;
            case WeatherIcons.CLOUDY:
                weatherIcon = R.drawable.cloudy;
                break;
            case WeatherIcons.CLOUDY_GUSTS:
                weatherIcon = R.drawable.cloudy_gusts;
                break;
            case WeatherIcons.CLOUDY_WINDY:
                weatherIcon = R.drawable.cloudy_windy;
                break;
            case WeatherIcons.FOG:
                weatherIcon = R.drawable.fog;
                break;
            case WeatherIcons.HAIL:
                weatherIcon = R.drawable.hail;
                break;
            case WeatherIcons.RAIN:
                weatherIcon = R.drawable.rain;
                break;
            case WeatherIcons.RAIN_MIX:
                weatherIcon = R.drawable.rain_mix;
                break;
            case WeatherIcons.RAIN_WIND:
                weatherIcon = R.drawable.rain_wind;
                break;
            case WeatherIcons.SHOWERS:
                weatherIcon = R.drawable.showers;
                break;
            case WeatherIcons.SLEET:
                weatherIcon = R.drawable.sleet;
                break;
            case WeatherIcons.SNOW:
                weatherIcon = R.drawable.snow;
                break;
            case WeatherIcons.SPRINKLE:
                weatherIcon = R.drawable.sprinkle;
                break;
            case WeatherIcons.STORM_SHOWERS:
                weatherIcon = R.drawable.storm_showers;
                break;
            case WeatherIcons.THUNDERSTORM:
                weatherIcon = R.drawable.thunderstorm;
                break;
            case WeatherIcons.SNOW_WIND:
                weatherIcon = R.drawable.snow_wind;
                break;
            case WeatherIcons.SMOG:
                weatherIcon = R.drawable.smog;
                break;
            case WeatherIcons.SMOKE:
                weatherIcon = R.drawable.smoke;
                break;
            case WeatherIcons.LIGHTNING:
                weatherIcon = R.drawable.lightning;
                break;
            case WeatherIcons.DUST:
                weatherIcon = R.drawable.dust;
                break;
            case WeatherIcons.SNOWFLAKE_COLD:
                weatherIcon = R.drawable.snowflake_cold;
                break;
            case WeatherIcons.WINDY:
                weatherIcon = R.drawable.windy;
                break;
            case WeatherIcons.STRONG_WIND:
                weatherIcon = R.drawable.strong_wind;
                break;
            case WeatherIcons.SANDSTORM:
                weatherIcon = R.drawable.sandstorm;
                break;
            case WeatherIcons.HURRICANE:
                weatherIcon = R.drawable.hurricane;
                break;
            case WeatherIcons.TORNADO:
                weatherIcon = R.drawable.tornado;
                break;
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.na;
        }

        return weatherIcon;
    }

    @Override
    public final LocationProviderImpl getLocationProvider() {
        return locationProvider;
    }
}
