package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.model.Weather;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private SettingsManager settingsManager;

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
            public void registerAppSharedPreferenceListener() {

            }

            @Override
            public void unregisterAppSharedPreferenceListener() {

            }

            @Override
            public void registerAppSharedPreferenceListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {

            }

            @Override
            public void unregisterAppSharedPreferenceListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {

            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }

            @Override
            public Bundle getProperties() {
                return new Bundle();
            }

            @Override
            public SettingsManager getSettingsManager() {
                return new SettingsManager(appContext.getApplicationContext());
            }
        };

        SimpleLibrary.initialize(app);

        // Start logger
        Logger.init(appContext);

        settingsManager = app.getSettingsManager();
    }

    @Test
    public void getWeatherTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        settingsManager.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void updateLocationQueryTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        settingsManager.setAPI(WeatherAPI.METNO);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);

        settingsManager.setAPI(WeatherAPI.NWS);
        wm.updateAPI();

        if ((weather != null && !weather.getSource().equals(settingsManager.getAPI()))
                || (weather == null && locationData != null && !locationData.getWeatherSource().equals(settingsManager.getAPI()))) {
            // Update location query and source for new API
            String oldKey = locationData.getQuery();

            if (weather != null)
                locationData.setQuery(wm.updateLocationQuery(weather));
            else
                locationData.setQuery(wm.updateLocationQuery(locationData));

            locationData.setWeatherSource(settingsManager.getAPI());
        }

        weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void minDate() {
        LocalDateTime updateTime = DateTimeUtils.getLocalDateTimeMIN();
        assertNotNull(LocalDateTime.parse("1/1/1900 12:00:00 AM",
                DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.JAPAN)));
    }
}
