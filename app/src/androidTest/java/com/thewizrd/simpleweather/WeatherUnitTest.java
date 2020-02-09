package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

@RunWith(AndroidJUnit4.class)
public class WeatherUnitTest {
    @Before
    public void init() {
        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();

        ApplicationLib app = new ApplicationLib() {
            @Override
            public Context getAppContext() {
                return appContext.getApplicationContext();
            }

            @Override
            public SharedPreferences getPreferences() {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext());
            }

            @Override
            public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
                return null;
            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }
        };

        SimpleLibrary.init(app);
        AndroidThreeTen.init(appContext);

        // Start logger
        Logger.init(appContext);
        Settings.loadIfNeeded();
    }

    private Weather getWeather(WeatherProviderImpl providerImpl) throws WeatherException {
        LocationQueryViewModel location = providerImpl.getLocation(new WeatherUtils.Coordinate(47.6721646, -122.1706614));
        LocationData locData = new LocationData(location);
        return providerImpl.getWeather(locData);
    }

    @Test
    public void getHEREWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.HERE);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getYahooWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.YAHOO);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getMetNoWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.METNO);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getNWSWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.NWS);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getOWMWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getHEREOAuthToken() {
        String token = new AsyncTask<String>().await(new Callable<String>() {
            @Override
            public String call() {
                return HEREOAuthUtils.getBearerToken(true);
            }
        });
        Assert.assertTrue(!StringUtils.isNullOrWhitespace(token));
    }
}
