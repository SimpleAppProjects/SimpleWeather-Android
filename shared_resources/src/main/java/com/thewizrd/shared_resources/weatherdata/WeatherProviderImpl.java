package com.thewizrd.shared_resources.weatherdata;

import android.content.Context;

import androidx.annotation.Nullable;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.aqicn.AQICNProvider;
import com.thewizrd.shared_resources.weatherdata.nws.alerts.NWSAlertProvider;

import java.util.Collection;
import java.util.List;

public abstract class WeatherProviderImpl implements WeatherProviderImplInterface {
    protected LocationProviderImpl locationProvider;

    // Variables
    public abstract String getWeatherAPI();

    public abstract boolean isKeyRequired();

    public abstract boolean supportsWeatherLocale();

    public abstract boolean supportsAlerts();

    public abstract boolean needsExternalAlertData();

    /**
     * Retrieve a list of locations from the location provider
     *
     * @param ac_query The AutoComplete query used to search locations
     * @return A list of locations matching the query
     * @throws WeatherException Weather Exception
     */
    public final Collection<LocationQueryViewModel> getLocations(String ac_query) throws WeatherException {
        return locationProvider.getLocations(ac_query, getWeatherAPI());
    }

    /**
     * Retrieve a single location from the location provider
     *
     * @param coordinate The coordinate used to search the location data
     * @return A single location matching the provided coordinate
     * @throws WeatherException Weather Exception
     */
    public final LocationQueryViewModel getLocation(WeatherUtils.Coordinate coordinate) throws WeatherException {
        return locationProvider.getLocation(coordinate, getWeatherAPI());
    }

    /**
     * Retrieve weather data from the weather provider
     *
     * @param location_query Location query to retrieve weather data;
     *                       Query string is defined in {@link LocationQueryViewModel#updateLocationQuery()}
     * @param country_code   Country code for the location defined by location_query parameter
     * @return Weather data object
     * @throws WeatherException Weather Exception
     */
    public abstract Weather getWeather(String location_query, String country_code) throws WeatherException;

    /**
     * This method is used to update the weather data retrieved with the query
     * (see {@link WeatherProviderImpl#getWeather(String, String)})
     * <p>
     * Mostly used to update Weather data with time zone info from {@link LocationData} or
     * to update {@link LocationData} if itself is missing TZ data
     *
     * @param location Location Data object
     * @return updated Weather data object
     * @throws WeatherException Weather Exception
     */
    @Override
    public Weather getWeather(LocationData location) throws WeatherException {
        if (location == null || location.getQuery() == null)
            throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);

        Weather weather = getWeather(location.getQuery(), location.getCountryCode());

        if (StringUtils.isNullOrWhitespace(location.getTzLong())) {
            if (!StringUtils.isNullOrWhitespace(weather.getLocation().getTzLong())) {
                location.setTzLong(weather.getLocation().getTzLong());
            } else if (location.getLongitude() != 0 && location.getLatitude() != 0) {
                String tzId = TZDBCache.getTimeZone(location.getLatitude(), location.getLongitude());
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

        weather.getLocation().setLatitude((float) location.getLatitude());
        weather.getLocation().setLongitude((float) location.getLongitude());
        weather.getLocation().setTzShort(location.getTzShort());
        weather.getLocation().setTzOffset(location.getTzOffset());

        // Additional external data
        weather.getCondition().setAirQuality(new AQICNProvider().getAirQualityData(location));

        return weather;
    }

    /**
     * Query the alert provider for current available weather alerts (currently US-only supported)
     *
     * @param location The location data used to search for weather alerts
     * @return A collection of weather alerts currently available
     */
    @Override
    public List<WeatherAlert> getAlerts(LocationData location) {
        if ("US".equals(location.getCountryCode()))
            return new NWSAlertProvider().getAlerts(location);
        else
            return null;
    }

    /**
     * Query the weather provider if the provided key is valid
     *
     * @param key Provider key to check
     * @return boolean Is valid or not
     * @throws WeatherException Weather Exception
     */
    public abstract boolean isKeyValid(String key) throws WeatherException;

    public abstract String getAPIKey();

    // Utils Methods

    /**
     * Refresh/update the location data from the supported location provider
     * and commit update to the database
     *
     * Uses coordinate {@link LocationData#getLatitude()}, {@link LocationData#getLongitude()}
     * to query location provider for updated location data
     *
     * @param location Location data to update
     */
    @Override
    public final void updateLocationData(LocationData location) {
        locationProvider.updateLocationData(location, getWeatherAPI());
    }

    /**
     * Returns an location query supported by this weather provider
     *
     * @param weather Weather data used to retrieve updated query
     * @return Returns location query supported by this weather provider
     */
    public abstract String updateLocationQuery(Weather weather);

    /**
     * Returns an location query supported by this weather provider
     *
     * @param location Location data used to retrieve updated query
     * @return Returns location query supported by this weather provider
     */
    public abstract String updateLocationQuery(LocationData location);

    /**
     * Returns the locale code supported by this weather provider
     *
     * @param iso See {@link ULocale#getLanguage()}
     * @param name See {@link ULocale#toLanguageTag()}
     * @return The locale code supported by this provider
     */
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

    /**
     * Map the passed icon string to a localized weather condition string (if available)
     *
     * @param icon The {@link WeatherIcons} to map
     * @return A localized weather condition string (if available); returns {@code NULL} if provider already supports localized data
     */
    @Nullable
    @Override
    public String getWeatherCondition(String icon) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        switch (icon) {
            case WeatherIcons.DAY_SUNNY:
                return context.getString(R.string.weather_sunny);
            case WeatherIcons.NIGHT_CLEAR:
                return context.getString(R.string.weather_clear);
            case WeatherIcons.DAY_SUNNY_OVERCAST:
                return context.getString(R.string.weather_overcast);
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                return context.getString(R.string.weather_partlycloudy);
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.DAY_CLOUDY_HIGH:
                return context.getString(R.string.weather_cloudy);
            case WeatherIcons.DAY_SPRINKLE:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.SPRINKLE:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.SHOWERS:
                return context.getString(R.string.weather_rainshowers);
            case WeatherIcons.DAY_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.THUNDERSTORM:
            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
                return context.getString(R.string.weather_tstorms);
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.SLEET:
                return context.getString(R.string.weather_sleet);
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.SNOW:
                return context.getString(R.string.weather_snow);
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
            case WeatherIcons.SNOW_WIND:
                return context.getString(R.string.weather_heavysnow);
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
                return context.getString(R.string.weather_snow_tstorms);
            case WeatherIcons.HAIL:
            case WeatherIcons.DAY_HAIL:
            case WeatherIcons.NIGHT_ALT_HAIL:
                return context.getString(R.string.weather_hail);
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.RAIN:
                return context.getString(R.string.weather_rain);
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.FOG:
                return context.getString(R.string.weather_fog);
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
                return context.getString(R.string.weather_sleet_tstorms);
            case WeatherIcons.SNOWFLAKE_COLD:
                return context.getString(R.string.weather_cold);
            case WeatherIcons.DAY_HOT:
                return context.getString(R.string.weather_hot);
            case WeatherIcons.DAY_HAZE:
                return context.getString(R.string.weather_haze);
            case WeatherIcons.SMOKE:
                return context.getString(R.string.weather_smoky);
            case WeatherIcons.SANDSTORM:
            case WeatherIcons.DUST:
                return context.getString(R.string.weather_dust);
            case WeatherIcons.TORNADO:
                return context.getString(R.string.weather_tornado);
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.RAIN_MIX:
                return context.getString(R.string.weather_rainandsnow);
            case WeatherIcons.DAY_WINDY:
            case WeatherIcons.WINDY:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.STRONG_WIND:
                return context.getString(R.string.weather_windy);
            case WeatherIcons.HURRICANE:
                return context.getString(R.string.weather_tropicalstorm);
            default:
                return context.getString(R.string.weather_notavailable);
        }
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
    public final LocationProviderImpl getLocationProvider() {
        return locationProvider;
    }
}
