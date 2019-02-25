package com.thewizrd.shared_resources.utils;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.helpers.WearableDataSync;
import com.thewizrd.shared_resources.weatherdata.Favorites;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class Settings {
    public static final String TAG = "Settings";

    // Database
    private static LocationsDatabase locationDB;
    private static WeatherDatabase weatherDB;

    // Data
    public static final int CURRENT_DBVERSION = 3;
    private static final int CACHE_LIMIT = 10;

    // Units
    private static final String FAHRENHEIT = "F";
    private static final String CELSIUS = "C";

    private static final String DEFAULT_UPDATE_INTERVAL;
    public static final int DEFAULTINTERVAL;

    private static final boolean IS_PHONE;

    // Shared Settings
    private static final SharedPreferences preferences;
    private static final SharedPreferences.Editor editor;
    private static final SharedPreferences wuSharedPrefs;
    private static final SharedPreferences versionPrefs;

    // Settings Keys
    private static final String KEY_API = "API";
    private static final String KEY_APIKEY = "API_KEY";
    private static final String KEY_APIKEY_VERIFIED = "API_KEY_VERIFIED";
    private static final String KEY_USECELSIUS = "key_usecelsius";
    private static final String KEY_UNITS = "Units";
    private static final String KEY_WEATHERLOADED = "weatherLoaded";
    private static final String KEY_FOLLOWGPS = "key_followgps";
    private static final String KEY_LASTGPSLOCATION = "key_lastgpslocation";
    private static final String KEY_REFRESHINTERVAL = "key_refreshinterval";
    private static final String KEY_UPDATETIME = "key_updatetime";
    private static final String KEY_DBVERSION = "key_dbversion";
    private static final String KEY_USEALERTS = "key_usealerts";
    private static final String KEY_USEPERSONALKEY = "key_usepersonalkey";
    private static final String KEY_CURRENTVERSION = "key_currentversion";
    // !ANDROID_WEAR
    private static final String KEY_ONGOINGNOTIFICATION = "key_ongoingnotification";
    private static final String KEY_NOTIFICATIONICON = "key_notificationicon";
    public static final String TEMPERATURE_ICON = "0";
    public static final String CONDITION_ICON = "1";
    // END - !ANDROID_WEAR
    // ANDROID_WEAR - only
    private static final String KEY_DATASYNC = "key_datasync";
    // END

    // App data files
    private static final File appDataFolder;

    // Weather Data
    private static LocationData lastGPSLocData;
    private static boolean loaded = false;

    // Shared Preferences listener
    public static class SettingsListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private LocalBroadcastManager mLocalBroadcastManager;
        private Context mContext;

        public SettingsListener(Context context) {
            mContext = context.getApplicationContext();
            mLocalBroadcastManager = LocalBroadcastManager
                    .getInstance(mContext);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (StringUtils.isNullOrWhitespace(key))
                return;

            switch (key) {
                // Weather Provider changed
                case KEY_API:
                    WeatherManager.getInstance().updateAPI();
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI));
                    break;
                // FollowGPS changed
                case KEY_FOLLOWGPS:
                    boolean value = sharedPreferences.getBoolean(key, false);
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS));
                    if (Settings.IS_PHONE)
                        mLocalBroadcastManager.sendBroadcast(
                                new Intent(value ? CommonActions.ACTION_WIDGET_REFRESHWIDGETS : CommonActions.ACTION_WIDGET_RESETWIDGETS));
                    break;
                // Settings unit changed
                case KEY_USECELSIUS:
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT));
                    break;
                // Refresh interval changed
                case KEY_REFRESHINTERVAL:
                    mLocalBroadcastManager.sendBroadcast(
                            new Intent(CommonActions.ACTION_SETTINGS_UPDATEREFRESH));
                    break;
                // Data sync setting changed
                case KEY_DATASYNC:
                    // Reset UpdateTime value to force a refresh
                    setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
                    break;
                default:
                    break;
            }
        }
    }

    static {
        if (SimpleLibrary.getInstance().getApp().isPhone()) {
            DEFAULT_UPDATE_INTERVAL = "60"; // 60 minutes (1hr)
            DEFAULTINTERVAL = 60;
            IS_PHONE = true;
        } else {
            DEFAULT_UPDATE_INTERVAL = "120"; // 120 minutes (2hrs)
            DEFAULTINTERVAL = 120;
            IS_PHONE = false;
        }

        appDataFolder = SimpleLibrary.getInstance().getAppContext().getFilesDir();
        preferences = SimpleLibrary.getInstance().getApp().getPreferences();
        editor = preferences.edit();
        wuSharedPrefs = SimpleLibrary.getInstance().getAppContext()
                .getSharedPreferences(WeatherAPI.WEATHERUNDERGROUND, Context.MODE_PRIVATE);
        versionPrefs = SimpleLibrary.getInstance().getAppContext()
                .getSharedPreferences("version", Context.MODE_PRIVATE);

        lastGPSLocData = new LocationData();

        init();
    }

    // Initialize file
    private static synchronized void init() {
        // Move database files for Room
        Context context = SimpleLibrary.getInstance().getAppContext();

        Logger.writeLine(Log.DEBUG, "init");

        if (getDBVersion() < 3) {
            File oldWeatherDB = new File(appDataFolder, "weatherdata.db");
            File newWeatherDB = context.getDatabasePath("weatherdata.db");
            if (oldWeatherDB.exists()) {
                Logger.writeLine(Log.DEBUG, "weatherdb: old path: " + oldWeatherDB.getAbsolutePath());
                Logger.writeLine(Log.DEBUG, "weatherdb: new path: " + newWeatherDB.getAbsolutePath());
                Logger.writeLine(Log.DEBUG, "weatherdb: " + oldWeatherDB.renameTo(newWeatherDB));
            }

            File oldLocDB = new File(appDataFolder, "locations.db");
            File newLocDB = context.getDatabasePath("locations.db");
            if (oldLocDB.exists()) {
                Logger.writeLine(Log.DEBUG, "locationsdb: old path: " + oldLocDB.getAbsolutePath());
                Logger.writeLine(Log.DEBUG, "locationsdb: new path: " + newLocDB.getAbsolutePath());
                Logger.writeLine(Log.DEBUG, "locationsdb: " + oldLocDB.renameTo(newLocDB));
            }
        }

        if (IS_PHONE && locationDB == null) {
            locationDB = Room.databaseBuilder(context,
                    LocationsDatabase.class, "locations.db")
//                    .addMigrations(MIGRATION_0_1, MIGRATION_1_2, MIGRATION_2_3)
                    .build();
        }

        if (weatherDB == null) {
            weatherDB = Room.databaseBuilder(context,
                    WeatherDatabase.class, "weatherdata.db")
//                    .addMigrations(MIGRATION_0_1, MIGRATION_1_2, MIGRATION_2_3)
                    .build();
        }

        preferences.registerOnSharedPreferenceChangeListener(SimpleLibrary.getInstance().getApp().getSharedPreferenceListener());
    }

    private static void loadIfNeeded() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!loaded) {
                    load();
                    loaded = true;
                }
                return null;
            }
        });
    }

    private static void load() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                /* DB Migration */
                // Migrate old data if available
                if (getDBVersion() < CURRENT_DBVERSION) {
                    switch (getDBVersion()) {
                        // Move data from json to db
                        case 0:
                            // Not available here
                            break;
                        // Add and set tz_long column in db
                        case 1:
                            if (IS_PHONE && locationDB.locationsDAO().getLocationDataCount() > 0) {
                                DBUtils.setLocationData(locationDB, getAPI());
                            }
                            break;
                        // Room DB migration
                        case 2:
                            // Move db from appdata to db folder
                            // Handled in init method
                            break;
                        default:
                            break;
                    }

                    setDBVersion(CURRENT_DBVERSION);
                }

                if (!StringUtils.isNullOrWhitespace(getLastGPSLocation())) {
                    try {
                        JsonReader reader = new JsonReader(new StringReader(getLastGPSLocation()));
                        lastGPSLocData = LocationData.fromJson(reader);
                    } catch (Exception ex) {
                        Logger.writeLine(Log.ERROR, ex, "SimpleWeather: Settings.Load(): LastGPSLocation");
                    } finally {
                        if (lastGPSLocData == null || StringUtils.isNullOrWhitespace(lastGPSLocData.getTzLong()))
                            lastGPSLocData = new LocationData();
                    }
                }

                /* Version-specific Migration */
                Context context = SimpleLibrary.getInstance().getAppContext();
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (isWeatherLoaded() && getVersionCode() < packageInfo.versionCode) {
                    // v1.3.7 - Yahoo (YQL) is no longer in service
                    // Update location data from HERE Geocoder service
                    if (WeatherAPI.HERE.equals(Settings.getAPI()) && getVersionCode() < 271370400) {
                        if (IS_PHONE) {
                            DBUtils.setLocationData(locationDB, WeatherAPI.HERE);
                            saveLastGPSLocData(lastGPSLocData = new LocationData());
                        } else if (lastGPSLocData != null) {
                            WeatherManager.getProvider(WeatherAPI.HERE)
                                    .updateLocationData(lastGPSLocData);
                        }
                    }
                    // v1.3.8+ - Yahoo API is back in service (but updated)
                    // Update location data from current geocoder service
                    if (WeatherAPI.YAHOO.equals(Settings.getAPI()) && getVersionCode() < 271380000) {
                        if (IS_PHONE) {
                            DBUtils.setLocationData(locationDB, WeatherAPI.YAHOO);
                            saveLastGPSLocData(lastGPSLocData = new LocationData());
                        } else if (lastGPSLocData != null) {
                            WeatherManager.getProvider(WeatherAPI.YAHOO)
                                    .updateLocationData(lastGPSLocData);
                        }
                    }
                }
                setVersionCode(packageInfo.versionCode);
                return null;
            }
        });
    }

    private static final Migration MIGRATION_0_1 = new Migration(0, 1) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    // DAO interfacing methods
    public static List<LocationData> getFavorites() {
        return new AsyncTask<List<LocationData>>().await(new Callable<List<LocationData>>() {
            @Override
            public List<LocationData> call() throws Exception {
                loadIfNeeded();
                return locationDB.locationsDAO().getFavorites();
            }
        });
    }

    public static List<LocationData> getLocationData() {
        return new AsyncTask<List<LocationData>>().await(new Callable<List<LocationData>>() {
            @Override
            public List<LocationData> call() throws Exception {
                loadIfNeeded();
                return locationDB.locationsDAO().loadAllLocationData();
            }
        });
    }

    public static LocationData getLocation(final String key) {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() throws Exception {
                loadIfNeeded();
                return locationDB.locationsDAO().getLocation(key);
            }
        });
    }

    public static Weather getWeatherData(final String key) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                loadIfNeeded();
                return weatherDB.weatherDAO().getWeatherData(key);
            }
        });
    }

    public static Weather getWeatherDataByCoordinate(final LocationData location) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                loadIfNeeded();
                String query = String.format(Locale.ROOT, "\"latitude\":\"%s\",\"longitude\":\"%s\"",
                        Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
                return weatherDB.weatherDAO().getWeatherDataByCoord("%" + query + "%");
            }
        });
    }

    public static List<WeatherAlert> getWeatherAlertData(final String key) {
        return new AsyncTask<List<WeatherAlert>>().await(new Callable<List<WeatherAlert>>() {
            @Override
            public List<WeatherAlert> call() throws Exception {
                loadIfNeeded();

                List<WeatherAlert> alerts = null;

                try {
                    WeatherAlerts weatherAlertData = weatherDB.weatherDAO().getWeatherAlertData(key);

                    if (weatherAlertData != null && weatherAlertData.getAlerts() != null)
                        alerts = weatherAlertData.getAlerts();
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex, "SimpleWeather: Settings.GetWeatherAlertData()");
                } finally {
                    if (alerts == null)
                        alerts = new ArrayList<>();
                }

                return alerts;
            }
        });
    }

    public static LocationData getLastGPSLocData() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() throws Exception {
                loadIfNeeded();

                if (lastGPSLocData != null && lastGPSLocData.getLocationType() != LocationType.GPS)
                    lastGPSLocData.setLocationType(LocationType.GPS);

                return lastGPSLocData;
            }
        });
    }

    public static void saveWeatherData(final Weather weather) {
        if (weather != null && weather.isValid()) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    weatherDB.weatherDAO().insertWeatherData(weather);
                    return null;
                }
            });
        }
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (weatherDB.weatherDAO().getWeatherDataCount() > CACHE_LIMIT)
                    cleanupWeatherData();
            }
        });
    }

    public static void saveWeatherAlerts(final LocationData location, final List<WeatherAlert> alerts) {
        if (location != null && location.isValid()) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    WeatherAlerts alertData = new WeatherAlerts(location.getQuery(), alerts);
                    weatherDB.weatherDAO().insertWeatherAlertData(alertData);
                    return null;
                }
            });
        }

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (weatherDB.weatherDAO().getWeatherAlertDataCount() > CACHE_LIMIT)
                    cleanupWeatherAlertData();
            }
        });
    }

    private static void cleanupWeatherData() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<LocationData> locs = null;

                if (IS_PHONE) {
                    locs = locationDB.locationsDAO().loadAllLocationData();
                    if (useFollowGPS()) locs.add(lastGPSLocData);
                } else {
                    locs = Collections.singletonList(getHomeData());
                }

                List<Weather> data = weatherDB.weatherDAO().loadAllWeatherData();
                List<Weather> weatherToDelete = new ArrayList<>();

                for (Weather weather : data) {
                    boolean delete = true;

                    for (LocationData loc : locs) {
                        if (weather.getQuery().equals(loc.getQuery())) {
                            delete = false;
                            break;
                        }
                    }

                    if (delete)
                        weatherToDelete.add(weather);
                }

                for (Weather weather : weatherToDelete) {
                    weatherDB.weatherDAO().deleteWeatherDataByKey(weather.getQuery());
                }
            }
        });
    }

    private static void cleanupWeatherAlertData() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<LocationData> locs = null;

                if (IS_PHONE) {
                    locs = locationDB.locationsDAO().loadAllLocationData();
                    if (useFollowGPS()) locs.add(lastGPSLocData);
                } else {
                    locs = Collections.singletonList(getHomeData());
                }

                List<WeatherAlerts> data = weatherDB.weatherDAO().loadAllWeatherAlertData();
                List<WeatherAlerts> weatherToDelete = new ArrayList<>();

                for (WeatherAlerts alert : data) {
                    boolean delete = true;

                    for (LocationData loc : locs) {
                        if (alert.getQuery().equals(loc.getQuery())) {
                            delete = false;
                            break;
                        }
                    }

                    if (delete)
                        weatherToDelete.add(alert);
                }

                for (WeatherAlerts alertData : weatherToDelete) {
                    weatherDB.weatherDAO().deleteWeatherAlertDataByKey(alertData.getQuery());
                }
            }
        });
    }

    public static void saveLocationData(final List<LocationData> locationData) {
        if (locationData != null) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    List<Favorites> favs = new ArrayList<>(locationData.size());
                    for (LocationData loc : locationData) {
                        if (loc != null && loc.isValid()) {
                            locationDB.locationsDAO().insertLocationData(loc);
                            Favorites fav = new Favorites();
                            fav.setQuery(loc.getQuery());
                            fav.setPosition(locationData.indexOf(loc));
                            locationDB.locationsDAO().insertFavorite(fav);
                        }
                    }

                    List<LocationData> locs = locationDB.locationsDAO().loadAllLocationData();
                    List<LocationData> locToDelete = new ArrayList<>();

                    for (LocationData l : locs) {
                        boolean delete = true;

                        for (LocationData l2 : locationData) {
                            if (l2.equals(l)) {
                                delete = false;
                                break;
                            }
                        }

                        if (delete)
                            locToDelete.add(l);
                    }

                    int count = locToDelete.size();

                    if (count > 0) {
                        for (LocationData loc : locToDelete) {
                            locationDB.locationsDAO().deleteLocationDataByKey(loc.getQuery());
                            locationDB.locationsDAO().deleteFavoritesByKey(loc.getQuery());
                        }
                    }

                    return null;
                }
            });
        }
    }

    public static void addLocation(final LocationData location) {
        if (location != null && location.isValid()) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    locationDB.locationsDAO().insertLocationData(location);
                    int pos = locationDB.locationsDAO().getLocationDataCount();
                    Favorites fav = new Favorites();
                    fav.setQuery(location.getQuery());
                    fav.setPosition(pos);
                    locationDB.locationsDAO().insertFavorite(fav);

                    return null;
                }
            });
        }
    }

    public static void updateLocation(final LocationData location) {
        if (location != null && location.isValid()) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    locationDB.locationsDAO().updateLocationData(location);
                    return null;
                }
            });
        }
    }

    public static void updateLocationWithKey(final LocationData location, final String oldKey) {
        if (location != null && location.isValid() && !StringUtils.isNullOrWhitespace(oldKey)) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    // Get position from favorites table
                    List<Favorites> favs = locationDB.locationsDAO().loadAllFavorites();
                    Favorites fav = null;
                    for (Favorites f : favs) {
                        if (f.getQuery().equals(oldKey)) {
                            fav = f;
                            break;
                        }
                    }

                    if (fav == null) {
                        return null;
                    }

                    int pos = fav.getPosition();

                    // Remove location from table
                    locationDB.locationsDAO().deleteLocationDataByKey(oldKey);
                    locationDB.locationsDAO().deleteFavoritesByKey(oldKey);

                    // Add updated location with new query (pkey)
                    locationDB.locationsDAO().insertLocationData(location);
                    fav = new Favorites();
                    fav.setQuery(location.getQuery());
                    fav.setPosition(pos);
                    locationDB.locationsDAO().insertFavorite(fav);

                    return null;
                }
            });
        }
    }

    public static void deleteLocations() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                locationDB.locationsDAO().deleteAllLocationData();
                locationDB.locationsDAO().deleteAllFavoriteData();

                return null;
            }
        });
    }

    public static void deleteLocation(final String key) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    locationDB.locationsDAO().deleteLocationDataByKey(key);
                    locationDB.locationsDAO().deleteFavoritesByKey(key);
                    resetPostition();

                    return null;
                }
            });
        }
    }

    public static void moveLocation(final String key, final int toPos) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    locationDB.locationsDAO().updateFavPosition(key, toPos);

                    return null;
                }
            });
        }
    }

    private static void resetPostition() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                List<Favorites> favs = locationDB.locationsDAO().loadAllFavoritesByPosition();
                for (Favorites fav : favs) {
                    fav.setPosition(favs.indexOf(fav));
                    locationDB.locationsDAO().updateFavorite(fav);
                }

                return null;
            }
        });
    }

    public static void saveLastGPSLocData(LocationData data) {
        lastGPSLocData = data;
        setLastGPSLocation(lastGPSLocData == null ? null : lastGPSLocData.toJson());
    }

    public static LocationData getHomeData() {
        LocationData homeData = null;

        if (IS_PHONE) {
            if (useFollowGPS())
                homeData = getLastGPSLocData();
            else
                homeData = (getFavorites() == null || getFavorites().size() == 0) ? null : getFavorites().get(0);
        } else {
            homeData = getLastGPSLocData();

            if (homeData != null && !useFollowGPS())
                homeData.setLocationType(LocationType.SEARCH);
        }

        return homeData;
    }

    // Android Wear specific members
    @RequiresApi(Build.VERSION_CODES.M)
    public static void saveHomeData(LocationData data) {
        lastGPSLocData = data;
        setLastGPSLocation(lastGPSLocData == null ? null : lastGPSLocData.toJson());
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static WearableDataSync getDataSync() {
        return WearableDataSync.valueOf(Integer.valueOf(preferences.getString(KEY_DATASYNC, "0")));
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static void setDataSync(WearableDataSync value) {
        editor.putString(KEY_DATASYNC, Integer.toString(value.getValue()));
        editor.commit();
    }

    // Settings Members
    public static boolean isFahrenheit() {
        return FAHRENHEIT.equals(getTempUnit());
    }

    public static String getTempUnit() {
        if (!preferences.contains(KEY_USECELSIUS)) {
            return FAHRENHEIT;
        } else if (preferences.getBoolean(KEY_USECELSIUS, false)) {
            return CELSIUS;
        }

        return FAHRENHEIT;
    }

    public static void setTempUnit(String value) {
        if (CELSIUS.equals(value))
            editor.putBoolean(KEY_USECELSIUS, true);
        else
            editor.putBoolean(KEY_USECELSIUS, false);
    }

    public static boolean isWeatherLoaded() {
        if (IS_PHONE) {
            if (!DBUtils.locationDataExists(locationDB)) {
                if (!DBUtils.weatherDataExists(weatherDB)) {
                    setWeatherLoaded(false);
                    return false;
                }
            }
        } else {
            if (!DBUtils.weatherDataExists(weatherDB)) {
                setWeatherLoaded(false);
                return false;
            }
        }

        if (preferences.contains(KEY_WEATHERLOADED) && preferences.getBoolean(KEY_WEATHERLOADED, false)) {
            setWeatherLoaded(true);
            return true;
        } else {
            return false;
        }
    }

    public static void setWeatherLoaded(boolean isLoaded) {
        editor.putBoolean(KEY_WEATHERLOADED, isLoaded);
        editor.commit();
    }

    public static String getAPI() {
        if (!preferences.contains(KEY_API)) {
            setAPI(WeatherAPI.HERE);
            return WeatherAPI.HERE;
        } else
            return preferences.getString(KEY_API, null);
    }

    public static void setAPI(String api) {
        editor.putString(KEY_API, api);
        editor.commit();
    }

    public static String getAPIKEY() {
        if (!preferences.contains(KEY_APIKEY)) {
            return "";
        } else {
            return preferences.getString(KEY_APIKEY, null);
        }
    }

    public static void setAPIKEY(String key) {
        editor.putString(KEY_APIKEY, key);
        editor.commit();
    }

    public static boolean useFollowGPS() {
        if (!preferences.contains(KEY_FOLLOWGPS)) {
            setFollowGPS(false);
            return false;
        } else
            return preferences.getBoolean(KEY_FOLLOWGPS, false);
    }

    public static void setFollowGPS(boolean value) {
        editor.putBoolean(KEY_FOLLOWGPS, value);
        editor.commit();
    }

    public static String getLastGPSLocation() {
        return preferences.getString(KEY_LASTGPSLOCATION, null);
    }

    public static void setLastGPSLocation(String value) {
        editor.putString(KEY_LASTGPSLOCATION, value);
        editor.commit();
    }

    public static LocalDateTime getUpdateTime() {
        if (!preferences.contains(KEY_UPDATETIME))
            return DateTimeUtils.getLocalDateTimeMIN();
        else
            return LocalDateTime.parse(preferences.getString(KEY_UPDATETIME, "1/1/1900 12:00:00 AM"),
                    DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT));
    }

    public static void setUpdateTime(LocalDateTime value) {
        editor.putString(KEY_UPDATETIME, value.format(DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.ROOT)));
        editor.commit();
    }

    // !ANDROID_WEAR
    public static int getRefreshInterval() {
        return Integer.parseInt(preferences.getString(KEY_REFRESHINTERVAL, DEFAULT_UPDATE_INTERVAL));
    }

    public static void setRefreshInterval(int value) {
        editor.putString(KEY_REFRESHINTERVAL, Integer.toString(value));
        editor.commit();
    }

    public static boolean showOngoingNotification() {
        if (!preferences.contains(KEY_ONGOINGNOTIFICATION))
            return false;
        else
            return preferences.getBoolean(KEY_ONGOINGNOTIFICATION, false);
    }

    public static String getNotificationIcon() {
        if (!preferences.contains(KEY_NOTIFICATIONICON)) {
            return TEMPERATURE_ICON;
        } else {
            return preferences.getString(KEY_NOTIFICATIONICON, TEMPERATURE_ICON);
        }
    }

    public static boolean useAlerts() {
        if (!preferences.contains(KEY_USEALERTS)) {
            setAlerts(false);
            return false;
        } else {
            return preferences.getBoolean(KEY_USEALERTS, false);
        }
    }

    public static void setAlerts(boolean value) {
        editor.putBoolean(KEY_USEALERTS, value);
        editor.commit();
    }
    // END - !ANDROID_WEAR

    private static int getDBVersion() {
        return Integer.parseInt(preferences.getString(KEY_DBVERSION, "0"));
    }

    private static void setDBVersion(int value) {
        editor.putString(KEY_DBVERSION, Integer.toString(value));
        editor.commit();
    }

    public static boolean isKeyVerified() {
        if (!wuSharedPrefs.contains(KEY_APIKEY_VERIFIED)) {
            return false;
        } else {
            return wuSharedPrefs.getBoolean(KEY_APIKEY_VERIFIED, false);
        }
    }

    public static void setKeyVerified(boolean value) {
        SharedPreferences.Editor wuEditor = wuSharedPrefs.edit();
        wuEditor.putBoolean(KEY_APIKEY_VERIFIED, value);
        wuEditor.apply();

        if (!value)
            wuEditor.remove(KEY_APIKEY_VERIFIED).apply();
    }

    public static boolean usePersonalKey() {
        if (!preferences.contains(KEY_USEPERSONALKEY))
            return false;
        else
            return preferences.getBoolean(KEY_USEPERSONALKEY, false);
    }

    public static void setPersonalKey(boolean value) {
        editor.putBoolean(KEY_USEPERSONALKEY, value);
        editor.commit();
    }

    private static int getVersionCode() {
        return Integer.parseInt(versionPrefs.getString(KEY_CURRENTVERSION, "0"));
    }

    private static void setVersionCode(int value) {
        SharedPreferences.Editor versionEditor = versionPrefs.edit();
        versionEditor.putString(KEY_CURRENTVERSION, Integer.toString(value));
        versionEditor.apply();
    }
}