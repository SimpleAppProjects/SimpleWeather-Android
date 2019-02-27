package com.thewizrd.shared_resources.weatherdata;

import android.location.Location;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.here.HEREWeatherProvider;
import com.thewizrd.shared_resources.weatherdata.metno.MetnoWeatherProvider;
import com.thewizrd.shared_resources.weatherdata.openweather.OpenWeatherMapProvider;
import com.thewizrd.shared_resources.weatherdata.weatherunderground.WeatherUndergroundProvider;
import com.thewizrd.shared_resources.weatherdata.weatheryahoo.YahooWeatherProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

// Wrapper class for supported Weather Providers
public class WeatherManager implements WeatherProviderImplInterface {
    private static WeatherManager instance;
    private static WeatherProviderImpl weatherProvider;
    private static Weather weatherData;

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

    // Shouldn't be used in anything other than WeatherNow
    public void updateWeather(Weather weather) {
        weatherData = weather;
    }

    // Static Methods
    public static WeatherProviderImpl getProvider(String API) {
        WeatherProviderImpl providerImpl = null;

        switch (API) {
            case WeatherAPI.YAHOO:
                providerImpl = new YahooWeatherProvider();
                break;
            case WeatherAPI.WEATHERUNDERGROUND:
                providerImpl = new WeatherUndergroundProvider();
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
            default:
                break;
        }

        if (providerImpl == null)
            throw new IllegalArgumentException("Argument API: Invalid API name! This API is not supported");

        return providerImpl;
    }

    public static boolean isKeyRequired(String API) {
        WeatherProviderImpl provider = null;
        boolean needsKey = false;

        provider = getProvider(API);

        needsKey = provider.isKeyRequired();
        provider = null;
        return needsKey;
    }

    public static boolean isKeyValid(final String key, final String API) {
        final WeatherProviderImpl provider = getProvider(API);
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
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
    public void updateLocationData(final LocationData location) {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                weatherProvider.updateLocationData(location);
                return null;
            }
        });
    }

    @Override
    public String updateLocationQuery(final Weather weather) {
        return new AsyncTask<String>().await(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return weatherProvider.updateLocationQuery(weather);
            }
        });
    }

    @Override
    public String updateLocationQuery(final LocationData location) {
        return new AsyncTask<String>().await(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return weatherProvider.updateLocationQuery(location);
            }
        });
    }

    @Override
    public Collection<LocationQueryViewModel> getLocations(final String ac_query) {
        return new AsyncTask<Collection<LocationQueryViewModel>>().await(new Callable<Collection<LocationQueryViewModel>>() {
            @Override
            public Collection<LocationQueryViewModel> call() throws Exception {
                return weatherProvider.getLocations(ac_query);
            }
        });
    }

    public LocationQueryViewModel getLocation(final Location location) {
        return new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
            @Override
            public LocationQueryViewModel call() throws Exception {
                return weatherProvider.getLocation(new WeatherUtils.Coordinate(location));
            }
        });
    }

    @Override
    public LocationQueryViewModel getLocation(final WeatherUtils.Coordinate coordinate) {
        return new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
            @Override
            public LocationQueryViewModel call() throws Exception {
                return weatherProvider.getLocation(coordinate);
            }
        });
    }

    @Override
    public LocationQueryViewModel getLocation(final String query) {
        return new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
            @Override
            public LocationQueryViewModel call() throws Exception {
                return weatherProvider.getLocation(query);
            }
        });
    }

    @Override
    public Weather getWeather(final String location_query) throws WeatherException {
        return new AsyncTaskEx<Weather, WeatherException>().await(new CallableEx<Weather, WeatherException>() {
            @Override
            public Weather call() throws WeatherException {
                return weatherProvider.getWeather(location_query);
            }
        });
    }

    @Override
    public Weather getWeather(final LocationData location) throws WeatherException {
        return new AsyncTaskEx<Weather, WeatherException>().await(new CallableEx<Weather, WeatherException>() {
            @Override
            public Weather call() throws WeatherException {
                return weatherProvider.getWeather(location);
            }
        });
    }

    @Override
    public List<WeatherAlert> getAlerts(final LocationData location) {
        return new AsyncTask<List<WeatherAlert>>().await(new Callable<List<WeatherAlert>>() {
            @Override
            public List<WeatherAlert> call() {
                return weatherProvider.getAlerts(location);
            }
        });
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
    public boolean isKeyValid(final String key) {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return weatherProvider.isKeyValid(key);
            }
        });
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
    public String getWeatherBackgroundURI(Weather weather) {
        return weatherProvider.getWeatherBackgroundURI(weather);
    }

    @Override
    public int getWeatherBackgroundColor(Weather weather) {
        return weatherProvider.getWeatherBackgroundColor(weather);
    }

    @Override
    public int getWeatherIconResource(String icon) {
        return weatherProvider.getWeatherIconResource(icon);
    }

    @Override
    public LocationProviderImpl getLocationProvider() {
        return weatherProvider.getLocationProvider();
    }
}
