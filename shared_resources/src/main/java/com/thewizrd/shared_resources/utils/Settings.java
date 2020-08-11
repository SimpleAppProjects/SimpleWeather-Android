package com.thewizrd.shared_resources.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.database.LocationsDAO;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDAO;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.weatherdata.Favorites;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.LocationType;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class Settings {
    public static final String TAG = "Settings";

    // Database
    private static LocationsDatabase locationDB;
    private static WeatherDatabase weatherDB;
    // NOTE: Remember to add migrations for all databases when updating version
    public static final int CURRENT_DBVERSION = 6;

    // Data
    private static final int CACHE_LIMIT = 25;
    private static final int MAX_LOCATIONS = 10;

    // Units
    public static final String FAHRENHEIT = "F";
    public static final String CELSIUS = "C";

    private static final String DEFAULT_UPDATE_INTERVAL;
    public static final int DEFAULTINTERVAL;

    static final boolean IS_PHONE;
    public static final int CONNECTION_TIMEOUT = 10000; // 10s
    public static final int READ_TIMEOUT = 10000; // 10s

    // Shared Settings
    private static final SharedPreferences preferences;
    private static final SharedPreferences.Editor editor;
    private static final SharedPreferences wuSharedPrefs;
    private static final SharedPreferences versionPrefs;

    // Settings Keys
    public static final String KEY_API = "API";
    public static final String KEY_APIKEY = "API_KEY";
    public static final String KEY_APIKEY_VERIFIED = "API_KEY_VERIFIED";
    public static final String KEY_USECELSIUS = "key_usecelsius";
    public static final String KEY_UNITS = "Units";
    private static final String KEY_WEATHERLOADED = "weatherLoaded";
    public static final String KEY_FOLLOWGPS = "key_followgps";
    private static final String KEY_LASTGPSLOCATION = "key_lastgpslocation";
    public static final String KEY_REFRESHINTERVAL = "key_refreshinterval";
    private static final String KEY_UPDATETIME = "key_updatetime";
    private static final String KEY_DBVERSION = "key_dbversion";
    public static final String KEY_USEALERTS = "key_usealerts";
    public static final String KEY_USEPERSONALKEY = "key_usepersonalkey";
    private static final String KEY_CURRENTVERSION = "key_currentversion";
    private static final String KEY_REQUESTED_BGLOCATION_ACCESS = "key_requested_bglocation_access";
    // !ANDROID_WEAR
    public static final String KEY_ONGOINGNOTIFICATION = "key_ongoingnotification";
    public static final String KEY_NOTIFICATIONICON = "key_notificationicon";
    private static final String KEY_ONBOARDINGCOMPLETE = "key_onboardcomplete";
    public static final String KEY_USERTHEME = "key_usertheme";
    public static final String TEMPERATURE_ICON = "0";
    public static final String CONDITION_ICON = "1";
    // END - !ANDROID_WEAR
    // ANDROID_WEAR - only
    public static final String KEY_DATASYNC = "key_datasync";
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
                    WearableDataSync dataSync = WearableDataSync.valueOf(Integer.parseInt(sharedPreferences.getString(KEY_DATASYNC, "0")));
                    setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
                    // Reset interval if setting is off
                    if (dataSync == WearableDataSync.OFF) setRefreshInterval(DEFAULTINTERVAL);
                    break;
                default:
                    break;
            }
        }
    }

    static {
        IS_PHONE = SimpleLibrary.getInstance().getApp().isPhone();

        DEFAULT_UPDATE_INTERVAL = "120"; // 120 minutes (2hrs)
        DEFAULTINTERVAL = 120;

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
                    .addMigrations(DBMigrations.MIGRATION_0_3, DBMigrations.LOC_MIGRATION_3_4, DBMigrations.LOC_MIGRATION_4_5, DBMigrations.LOC_MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        if (weatherDB == null) {
            weatherDB = Room.databaseBuilder(context,
                    WeatherDatabase.class, "weatherdata.db")
                    .addMigrations(DBMigrations.MIGRATION_0_3, DBMigrations.W_MIGRATION_3_4, DBMigrations.W_MIGRATION_4_5, DBMigrations.W_MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        preferences.registerOnSharedPreferenceChangeListener(SimpleLibrary.getInstance().getApp().getSharedPreferenceListener());
    }

    public static void loadIfNeeded() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
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
            public Void call() {
                /* DB Migration */
                DBMigrations.performMigrations(weatherDB, locationDB);

                if (!StringUtils.isNullOrWhitespace(getLastGPSLocation())) {
                    try {
                        JsonReader reader = new JsonReader(new StringReader(getLastGPSLocation()));
                        lastGPSLocData = new LocationData();
                        lastGPSLocData.fromJson(reader);
                    } catch (Exception ex) {
                        Logger.writeLine(Log.ERROR, ex, "SimpleWeather: Settings.Load(): LastGPSLocation");
                    } finally {
                        if (lastGPSLocData == null || StringUtils.isNullOrWhitespace(lastGPSLocData.getTzLong()))
                            lastGPSLocData = new LocationData();
                    }
                }

                /* Version-specific Migration */
                VersionMigrations.performMigrations(weatherDB, locationDB);
                return null;
            }
        });
    }

    // DAO interfacing methods
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static WeatherDAO getWeatherDAO() {
        return weatherDB.weatherDAO();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static LocationsDAO getLocationsDAO() {
        return locationDB.locationsDAO();
    }

    public static Collection<LocationData> getFavorites() {
        return new AsyncTask<List<LocationData>>().await(new Callable<List<LocationData>>() {
            @Override
            public List<LocationData> call() {
                loadIfNeeded();
                return locationDB.locationsDAO().getFavorites();
            }
        });
    }

    public static List<LocationData> getLocationData() {
        return new AsyncTask<List<LocationData>>().await(new Callable<List<LocationData>>() {
            @Override
            public List<LocationData> call() {
                loadIfNeeded();
                return locationDB.locationsDAO().loadAllLocationData();
            }
        });
    }

    public static LocationData getLocation(final String key) {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() {
                loadIfNeeded();
                return locationDB.locationsDAO().getLocation(key);
            }
        });
    }

    public static Weather getWeatherData(final String key) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                loadIfNeeded();
                return weatherDB.weatherDAO().getWeatherData(key);
            }
        });
    }

    public static Weather getWeatherDataByCoordinate(final LocationData location) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                loadIfNeeded();
                String query = String.format(Locale.ROOT, "\"latitude\":\"%s\",\"longitude\":\"%s\"",
                        Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
                return weatherDB.weatherDAO().getWeatherDataByCoord("%" + query + "%");
            }
        });
    }

    public static Collection<WeatherAlert> getWeatherAlertData(final String key) {
        return new AsyncTask<Collection<WeatherAlert>>().await(new Callable<Collection<WeatherAlert>>() {
            @Override
            public Collection<WeatherAlert> call() {
                loadIfNeeded();

                Collection<WeatherAlert> alerts = null;

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

    public static Forecasts getWeatherForecastData(final String key) {
        return new AsyncTask<Forecasts>().await(new Callable<Forecasts>() {
            @Override
            public Forecasts call() {
                loadIfNeeded();

                return weatherDB.weatherDAO().getForecastData(key);
            }
        });
    }

    public static List<HourlyForecast> getHourlyWeatherForecastDataByLimit(final String key, final int loadSize) {
        return new AsyncTask<List<HourlyForecast>>().await(new Callable<List<HourlyForecast>>() {
            @Override
            public List<HourlyForecast> call() {
                loadIfNeeded();

                return weatherDB.weatherDAO().getHourlyForecastsByQueryOrderByDateByLimit(key, loadSize);
            }
        });
    }

    public static List<HourlyForecast> getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(final String key, final int loadSize, final ZonedDateTime date) {
        return new AsyncTask<List<HourlyForecast>>().await(new Callable<List<HourlyForecast>>() {
            @Override
            public List<HourlyForecast> call() {
                loadIfNeeded();

                return weatherDB.weatherDAO().getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(key, loadSize, date);
            }
        });
    }

    public static List<HourlyForecast> getHourlyWeatherForecastData(final String key) {
        return new AsyncTask<List<HourlyForecast>>().await(new Callable<List<HourlyForecast>>() {
            @Override
            public List<HourlyForecast> call() {
                loadIfNeeded();

                return weatherDB.weatherDAO().getHourlyForecastsByQueryOrderByDate(key);
            }
        });
    }

    public static HourlyForecast getFirstHourlyForecastDataByDate(final String key, final ZonedDateTime date) {
        return new AsyncTask<HourlyForecast>().await(new Callable<HourlyForecast>() {
            @Override
            public HourlyForecast call() {
                loadIfNeeded();

                return weatherDB.weatherDAO().getFirstHourlyForecastDataByDate(key, date);
            }
        });
    }

    public static LocationData getLastGPSLocData() {
        return new AsyncTask<LocationData>().await(new Callable<LocationData>() {
            @Override
            public LocationData call() {
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
                public Void call() {
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

    public static void saveWeatherAlerts(final LocationData location, final Collection<WeatherAlert> alerts) {
        if (location != null && location.isValid()) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
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

    public static void saveWeatherForecasts(final Forecasts forecasts) {
        if (forecasts != null) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    weatherDB.weatherDAO().insertForecast(forecasts);
                    return null;
                }
            });
        }
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (weatherDB.weatherDAO().getForecastDataCountGroupedByQuery() > CACHE_LIMIT / 2)
                    cleanupWeatherForecastData();
            }
        });
    }

    public static void saveWeatherForecasts(@NonNull final String key, final Collection<HourlyForecasts> forecasts) {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                weatherDB.weatherDAO().deleteHourlyForecastByKey(key);

                if (forecasts != null) {
                    weatherDB.weatherDAO().insertAllHourlyForecasts(forecasts);
                }
                return null;
            }
        });

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (weatherDB.weatherDAO().getHourlyForecastCountGroupedByQuery() > CACHE_LIMIT / 2)
                    cleanupWeatherForecastData();
            }
        });
    }

    private static void cleanupWeatherData() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<LocationData> locs;

                if (IS_PHONE) {
                    locs = locationDB.locationsDAO().loadAllLocationData();
                    if (useFollowGPS()) locs.add(lastGPSLocData);
                } else {
                    locs = Collections.singletonList(getHomeData());
                }

                List<String> locQueries = Lists.transform(locs, new Function<LocationData, String>() {
                    @NullableDecl
                    @Override
                    public String apply(@NullableDecl LocationData input) {
                        return input.getQuery();
                    }
                });

                weatherDB.weatherDAO().deleteWeatherDataByKeyNotIn(locQueries);
            }
        });
    }

    private static void cleanupWeatherForecastData() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<LocationData> locs;

                if (IS_PHONE) {
                    locs = locationDB.locationsDAO().loadAllLocationData();
                    if (useFollowGPS()) locs.add(lastGPSLocData);
                } else {
                    locs = Collections.singletonList(getHomeData());
                }

                List<String> locQueries = Lists.transform(locs, new Function<LocationData, String>() {
                    @NullableDecl
                    @Override
                    public String apply(@NullableDecl LocationData input) {
                        return input.getQuery();
                    }
                });

                weatherDB.weatherDAO().deleteForecastByKeyNotIn(locQueries);
                weatherDB.weatherDAO().deleteHourlyForecastByKeyNotIn(locQueries);
            }
        });
    }

    private static void cleanupWeatherAlertData() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                List<LocationData> locs;

                if (IS_PHONE) {
                    locs = locationDB.locationsDAO().loadAllLocationData();
                    if (useFollowGPS()) locs.add(lastGPSLocData);
                } else {
                    locs = Collections.singletonList(getHomeData());
                }

                List<String> locQueries = Lists.transform(locs, new Function<LocationData, String>() {
                    @NullableDecl
                    @Override
                    public String apply(@NullableDecl LocationData input) {
                        return input.getQuery();
                    }
                });

                weatherDB.weatherDAO().deleteWeatherAlertDataByKeyNotIn(locQueries);
            }
        });
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
        if (location != null && location.getLocationType() == LocationType.GPS && location.isValid()) {
            Settings.saveLastGPSLocData(location);
        } else if (location != null && location.getLocationType() == LocationType.SEARCH && location.isValid()) {
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
                    Favorites fav = locationDB.locationsDAO().getFavorite(oldKey);

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
        setLastGPSLocation(JSONParser.serializer(lastGPSLocData, LocationData.class));
    }

    public static LocationData getHomeData() {
        LocationData homeData;

        if (IS_PHONE) {
            if (useFollowGPS())
                homeData = getLastGPSLocData();
            else
                homeData = new AsyncTask<LocationData>().await(new Callable<LocationData>() {
                    @Override
                    public LocationData call() {
                        loadIfNeeded();
                        return locationDB.locationsDAO().getFirstFavorite();
                    }
                });
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
        setLastGPSLocation(JSONParser.serializer(lastGPSLocData, LocationData.class));
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public static WearableDataSync getDataSync() {
        return WearableDataSync.valueOf(Integer.parseInt(preferences.getString(KEY_DATASYNC, "0")));
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
                setWeatherLoaded(false);
                return false;
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

    public static void setOngoingNotification(boolean value) {
        editor.putBoolean(KEY_ONGOINGNOTIFICATION, value);
        editor.commit();
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

    public static UserThemeMode getUserThemeMode() {
        if (!preferences.contains(KEY_USERTHEME)) {
            setUserThemeMode(UserThemeMode.FOLLOW_SYSTEM);
            return UserThemeMode.FOLLOW_SYSTEM;
        } else {
            return UserThemeMode.valueOf(Integer.parseInt(preferences.getString(KEY_USERTHEME, "0")));
        }
    }

    public static void setUserThemeMode(UserThemeMode value) {
        editor.putString(KEY_USERTHEME, Integer.toString(value.getValue()));
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean requestedBGAccess() {
        return preferences.getBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void setRequestBGAccess(boolean value) {
        editor.putBoolean(Manifest.permission.ACCESS_BACKGROUND_LOCATION, value);
        editor.commit();
    }
    // END - !ANDROID_WEAR

    static int getDBVersion() {
        return Integer.parseInt(preferences.getString(KEY_DBVERSION, "0"));
    }

    static void setDBVersion(int value) {
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

    static long getVersionCode() {
        return Long.parseLong(versionPrefs.getString(KEY_CURRENTVERSION, "0"));
    }

    static void setVersionCode(long value) {
        SharedPreferences.Editor versionEditor = versionPrefs.edit();
        versionEditor.putString(KEY_CURRENTVERSION, Long.toString(value));
        versionEditor.apply();
    }

    public static int getMaxLocations() {
        return MAX_LOCATIONS;
    }

    public static boolean isOnBoardingComplete() {
        loadIfNeeded();

        if (!preferences.contains(KEY_ONBOARDINGCOMPLETE))
            return false;
        else
            return preferences.getBoolean(KEY_ONBOARDINGCOMPLETE, false);
    }

    public static void setOnBoardingComplete(boolean value) {
        editor.putBoolean(KEY_ONBOARDINGCOMPLETE, value);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static float getAnimatorScale() {
        return android.provider.Settings.Global.getFloat(
                SimpleLibrary.getInstance().getAppContext().getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f);
    }
}