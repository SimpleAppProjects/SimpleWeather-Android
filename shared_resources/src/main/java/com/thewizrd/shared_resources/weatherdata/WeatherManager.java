package com.thewizrd.shared_resources.weatherdata;

import android.location.Location;

import androidx.annotation.WorkerThread;

import com.thewizrd.shared_resources.BuildConfig;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.here.HEREWeatherProvider;
import com.thewizrd.shared_resources.weatherdata.metno.MetnoWeatherProvider;
import com.thewizrd.shared_resources.weatherdata.nws.NWSWeatherProvider;
import com.thewizrd.shared_resources.weatherdata.openweather.OpenWeatherMapProvider;
import com.thewizrd.shared_resources.weatherdata.weatherunlocked.WeatherUnlockedProvider;
import com.thewizrd.shared_resources.weatherdata.weatheryahoo.YahooWeatherProvider;

import java.util.Collection;

// Wrapper class for supported Weather Providers
public final class WeatherManager implements WeatherProviderImplInterface {
    private static WeatherManager instance;
    private static WeatherProviderImpl weatherProvider;

    // Prevent instance from being created outside of this class
    private WeatherManager() {
        updateAPI();
    }

    public static synchronized WeatherManager getInstance() {
        if (instance == null)
            instance = new WeatherManager();

        return instance;
    }

    public void updateAPI() {
        String API = Settings.getAPI();
        weatherProvider = getProvider(API);
    }

    // Static Methods
    public static WeatherProviderImpl getProvider(String API) {
        WeatherProviderImpl providerImpl = null;

        switch (API) {
            case WeatherAPI.YAHOO:
                providerImpl = new YahooWeatherProvider();
                break;
            case WeatherAPI.HERE:
                providerImpl = new HEREWeatherProvider();
                break;
            case WeatherAPI.OPENWEATHERMAP:
                providerImpl = new OpenWeatherMapProvider();
                break;
            case WeatherAPI.METNO:
                providerImpl = new MetnoWeatherProvider();
                break;
            case WeatherAPI.NWS:
                providerImpl = new NWSWeatherProvider();
                break;
            case WeatherAPI.WEATHERUNLOCKED:
                providerImpl = new WeatherUnlockedProvider();
                break;
            default:
                if (!BuildConfig.DEBUG) {
                    providerImpl = new WeatherUnlockedProvider();
                }
                break;
        }

        if (providerImpl == null)
            throw new IllegalArgumentException("Argument API: Invalid API name! This API is not supported");

        return providerImpl;
    }

    public static boolean isKeyRequired(String API) {
        WeatherProviderImpl provider = getProvider(API);
        return provider.isKeyRequired();
    }

    public static boolean isKeyValid(final String key, final String API) throws WeatherException {
        final WeatherProviderImpl provider = getProvider(API);
        return AsyncTask.await(new CallableEx<Boolean, WeatherException>() {
            @Override
            public Boolean call() throws WeatherException {
                return provider.isKeyValid(key);
            }
        });
    }

    // Provider dependent methods
    @Override
    public String getWeatherAPI() {
        return weatherProvider.getWeatherAPI();
    }

    @Override
    public boolean isKeyRequired() {
        return weatherProvider.isKeyRequired();
    }

    @Override
    public boolean supportsWeatherLocale() {
        return weatherProvider.supportsWeatherLocale();
    }

    @Override
    public boolean supportsAlerts() {
        return weatherProvider.supportsAlerts();
    }

    @Override
    public boolean needsExternalAlertData() {
        return weatherProvider.needsExternalAlertData();
    }

    @Override
    public void updateLocationData(LocationData location) {
        weatherProvider.updateLocationData(location);
    }

    @Override
    public String updateLocationQuery(final Weather weather) {
        return weatherProvider.updateLocationQuery(weather);
    }

    @Override
    public String updateLocationQuery(final LocationData location) {
        return weatherProvider.updateLocationQuery(location);
    }

    @WorkerThread
    @Override
    public Collection<LocationQueryViewModel> getLocations(final String ac_query) throws WeatherException {
        return weatherProvider.getLocations(ac_query);
    }

    @WorkerThread
    public LocationQueryViewModel getLocation(final Location location) throws WeatherException {
        return weatherProvider.getLocation(new WeatherUtils.Coordinate(location));
    }

    @WorkerThread
    @Override
    public LocationQueryViewModel getLocation(final WeatherUtils.Coordinate coordinate) throws WeatherException {
        return weatherProvider.getLocation(coordinate);
    }

    @WorkerThread
    @Override
    public Weather getWeather(final String location_query, final String country_code) throws WeatherException {
        return weatherProvider.getWeather(location_query, country_code);
    }

    @WorkerThread
    @Override
    public Weather getWeather(final LocationData location) throws WeatherException {
        return weatherProvider.getWeather(location);
    }

    @WorkerThread
    @Override
    public Collection<WeatherAlert> getAlerts(final LocationData location) {
        return weatherProvider.getAlerts(location);
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return weatherProvider.localeToLangCode(iso, name);
    }

    @Override
    public String getWeatherIcon(String icon) {
        return weatherProvider.getWeatherIcon(icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        return weatherProvider.getWeatherIcon(isNight, icon);
    }

    @Override
    public String getWeatherCondition(String icon) {
        return weatherProvider.getWeatherCondition(icon);
    }

    @Override
    public boolean isKeyValid(String key) throws WeatherException {
        return weatherProvider.isKeyValid(key);
    }

    @Override
    public String getAPIKey() {
        return weatherProvider.getAPIKey();
    }

    @Override
    public boolean isNight(Weather weather) {
        return weatherProvider.isNight(weather);
    }

    @Override
    public LocationProviderImpl getLocationProvider() {
        return weatherProvider.getLocationProvider();
    }
}
