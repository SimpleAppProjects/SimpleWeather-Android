package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ibm.icu.text.DateFormat;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.FileUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder;
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
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

    @Test
    public void updateWidgetTest() {
        WidgetUtils.addWidgetId("NewYork", 10);
        WidgetUtils.addWidgetId("NewYork", 11);
        WidgetUtils.addWidgetId("NewYork", 12);
        WidgetUtils.addWidgetId("NewYork", 13);
        WidgetUtils.addWidgetId("NewYork", 14);
        WidgetUtils.addWidgetId("NewYork", 15);
        WidgetUtils.addWidgetId("NewYork", 16);
        WidgetUtils.addWidgetId("NewYork", 17);

        LocationData loc = new LocationData();
        loc.setQuery("OldYork");

        WidgetUtils.updateWidgetIds("NewYork", loc);
    }

    @Test
    public void getWeatherTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        // Need to get FULL location data for HERE API
        // Data provided is incomplete
        if (loc.getLocationLat() == -1 && loc.getLocationLong() == -1
                && loc.getLocationTZLong() == null
                && wm.getLocationProvider().needsLocationFromID()) {
            final LocationQueryViewModel query_vm = loc;
            loc = AsyncTask.await(new Callable<LocationQueryViewModel>() {
                @Override
                public LocationQueryViewModel call() throws WeatherException {
                    return wm.getLocationProvider().getLocationFromID(query_vm.getLocationQuery(), WeatherAPI.HERE);
                }
            });
        }

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void updateLocationQueryTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        // Need to get FULL location data for HERE API
        // Data provided is incomplete
        if (loc.getLocationLat() == -1 && loc.getLocationLong() == -1
                && loc.getLocationTZLong() == null
                && wm.getLocationProvider().needsLocationFromID()) {
            final LocationQueryViewModel query_vm = loc;
            loc = AsyncTask.await(new Callable<LocationQueryViewModel>() {
                @Override
                public LocationQueryViewModel call() throws WeatherException {
                    return wm.getLocationProvider().getLocationFromID(query_vm.getLocationQuery(), WeatherAPI.HERE);
                }
            });
        }

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);

        Settings.setAPI(WeatherAPI.YAHOO);
        wm.updateAPI();

        if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                || (weather == null && locationData != null && !locationData.getWeatherSource().equals(Settings.getAPI()))) {
            // Update location query and source for new API
            String oldKey = locationData.getQuery();

            if (weather != null)
                locationData.setQuery(wm.updateLocationQuery(weather));
            else
                locationData.setQuery(wm.updateLocationQuery(locationData));

            locationData.setWeatherSource(Settings.getAPI());
        }

        weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void widgetCleanupTest() {
        WidgetUtils.cleanupWidgetData();
        WidgetUtils.cleanupWidgetIds();
    }

    @Test
    public void notificationTest() {
        LocationQueryViewModel vm = new LocationQueryViewModel();
        vm.setLocationCountry("US");
        vm.setLocationName("New York, NY");
        vm.setLocationQuery("11413");
        vm.setLocationTZLong("America/New_York");
        List<WeatherAlert> la = new ArrayList<>();
        for (int i = 0; i < WeatherAlertType.values().length; i++) {
            WeatherAlert alert = new WeatherAlert();
            alert.setAttribution("Attribution");
            alert.setDate(ZonedDateTime.now(ZoneOffset.UTC));
            alert.setExpiresDate(ZonedDateTime.now(ZoneOffset.UTC).plusDays(5));
            alert.setMessage("Message");
            alert.setTitle("Title");
            alert.setType(WeatherAlertType.valueOf(i));
            alert.setNotified(false);
            la.add(alert);
        }
        WeatherAlertNotificationBuilder.createNotifications(new LocationData(vm), la);

        LocationQueryViewModel vm2 = new LocationQueryViewModel();
        vm2.setLocationCountry("US");
        vm2.setLocationName("New York City, NY");
        vm2.setLocationQuery("10007");
        vm2.setLocationTZLong("America/New_York");
        List<WeatherAlert> la2 = new ArrayList<>();
        for (int i = 0; i < WeatherAlertType.values().length; i++) {
            WeatherAlert alert = new WeatherAlert();
            alert.setAttribution("Attribution");
            alert.setDate(ZonedDateTime.now(ZoneOffset.UTC));
            alert.setExpiresDate(ZonedDateTime.now(ZoneOffset.UTC).plusDays(5));
            alert.setMessage("Message");
            alert.setTitle("Title");
            alert.setType(WeatherAlertType.valueOf(i));
            alert.setNotified(false);
            la2.add(alert);
        }
        WeatherAlertNotificationBuilder.createNotifications(new LocationData(vm2), la2);

        while (WeatherAlertNotificationService.getNotificationsCount() > 0) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
    }

    @Test
    public void logCleanupTest() throws IOException {
        // Context of the app under test.
        final Context appContext = SimpleLibrary.getInstance().getAppContext();

        String filePath = appContext.getExternalFilesDir(null) + "/logs";

        File directory = new File(filePath);

        if (!directory.exists()) {
            Assert.assertTrue(directory.mkdir());
        }

        for (int i = 0; i < 4; i++) {
            File file = new File(filePath + File.separator + "Log." + i + ".log");
            Assert.assertTrue(file.createNewFile());
        }

        Assert.assertTrue(FileUtils.deleteDirectory(filePath));
    }

    @Test
    public void timeIsRelative() {
        ZonedDateTime dateTime = ZonedDateTime.of(2020, 1, 1, 18, 0, 0, 0, ZoneId.systemDefault());
        Date date = DateTimeUtils.toDate(dateTime.toInstant());

        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.JAPAN).format(date));

        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.JAPAN).format(date));

        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.JAPAN).format(date));

        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.JAPAN).format(date));

        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.US)));
        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.FRANCE)));
        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.JAPAN)));

        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.JAPAN).format(date));

        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.US).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.FRANCE).format(date));
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.JAPAN).format(date));
    }
}
