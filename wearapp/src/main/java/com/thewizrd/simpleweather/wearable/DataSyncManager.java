package com.thewizrd.simpleweather.wearable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.ObjectsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thewizrd.shared_resources.icons.WeatherIconsProvider;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.wearable.WearableSettings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class DataSyncManager {
    @WorkerThread
    public static void updateSettings(@NonNull Context context, final DataMap dataMap) {
        context = context.getApplicationContext();

        if (dataMap != null && !dataMap.isEmpty()) {
            LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);

            long updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME);

            if (updateTimeMillis != getSettingsUpdateTime(context)) {
                String API = dataMap.getString(WearableSettings.KEY_API, "");
                String API_KEY = dataMap.getString(WearableSettings.KEY_APIKEY, "");
                boolean keyVerified = dataMap.getBoolean(WearableSettings.KEY_APIKEY_VERIFIED, false);
                if (!StringUtils.isNullOrWhitespace(API)) {
                    Settings.setAPI(API);
                    if (WeatherManager.isKeyRequired(API)) {
                        Settings.setAPIKEY(API_KEY);
                        Settings.setKeyVerified(false);
                    } else {
                        Settings.setAPIKEY("");
                        Settings.setKeyVerified(false);
                    }
                }

                Settings.setFollowGPS(dataMap.getBoolean(WearableSettings.KEY_FOLLOWGPS, false));

                DataMap unitMap = dataMap.getDataMap(WearableSettings.KEY_UNITS);
                final String oldUnits = Settings.getUnitString();
                if (unitMap != null) {
                    Settings.setTemperatureUnit(unitMap.getString(WearableSettings.KEY_TEMPUNIT, Units.FAHRENHEIT));
                    Settings.setSpeedUnit(unitMap.getString(WearableSettings.KEY_SPEEDUNIT, Units.MILES_PER_HOUR));
                    Settings.setDistanceUnit(unitMap.getString(WearableSettings.KEY_DISTANCEUNIT, Units.MILES));
                    Settings.setPressureUnit(unitMap.getString(WearableSettings.KEY_PRESSUREUNIT, Units.INHG));
                    Settings.setPrecipitationUnit(unitMap.getString(WearableSettings.KEY_PRECIPITATIONUNIT, Units.INCHES));
                } else {
                    Settings.setDefaultUnits(dataMap.getString(WearableSettings.KEY_TEMPUNIT, Units.FAHRENHEIT));
                }
                final String newUnits = Settings.getUnitString();

                if (!ObjectsCompat.equals(oldUnits, newUnits)) {
                    localBroadcastMgr.sendBroadcast(new Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT));
                }

                LocaleUtils.setLocaleCode(dataMap.getString(WearableSettings.KEY_LANGUAGE, ""));

                final String oldIcons = Settings.getIconsProvider();
                Settings.setIconsProvider(dataMap.getString(WearableSettings.KEY_ICONPROVIDER, WeatherIconsProvider.KEY));
                final String newIcons = Settings.getIconsProvider();
                if (!ObjectsCompat.equals(oldIcons, newIcons)) {
                    // Update tiles and complications
                    WidgetUpdaterWorker.requestWidgetUpdate(context);
                }

                setSettingsUpdateTime(context, updateTimeMillis);

                Timber.tag("DataSyncManager").d("Updated settings");
            }

            // Send callback to receiver
            localBroadcastMgr.sendBroadcast(new Intent(WearableHelper.SettingsPath));
        }
    }

    @WorkerThread
    public static void updateLocation(@NonNull Context context, final DataMap dataMap) {
        context = context.getApplicationContext();

        if (dataMap != null && !dataMap.isEmpty()) {
            String locationJSON = dataMap.getString(WearableSettings.KEY_LOCATIONDATA, "");
            if (!StringUtils.isNullOrWhitespace(locationJSON)) {
                LocationData locationData = JSONParser.deserializer(locationJSON, LocationData.class);

                if (locationData != null) {
                    long updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME);

                    if (updateTimeMillis != getLocationDataUpdateTime(context) ||
                            !locationData.equals(Settings.getHomeData())) {
                        Settings.saveHomeData(locationData);
                    }

                    setLocationDataUpdateTime(context, updateTimeMillis);

                    Timber.tag("DataSyncManager").d("updateLocation: Updated location data");

                    // Send callback to receiver
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            new Intent(WearableHelper.LocationPath));
                }
            }
        }
    }

    @WorkerThread
    public static void updateWeather(@NonNull Context context, final DataMap dataMap) {
        context = context.getApplicationContext();

        if (dataMap != null && !dataMap.isEmpty()) {
            long updateTimeMillis = dataMap.getLong(WearableSettings.KEY_UPDATETIME);
            // Check if data actually exists to force an update
            boolean dataExists = false;
            LocationData homeData = Settings.getHomeData();
            if (homeData != null) {
                dataExists = Settings.getWeatherDAO().getWeatherDataCountByKey(homeData.getQuery()) > 0;
            }

            if (updateTimeMillis != getWeatherUpdateTime(context) || !dataExists) {
                final Asset weatherAsset = dataMap.getAsset(WearableSettings.KEY_WEATHERDATA);
                if (weatherAsset != null) {
                    try (InputStream inputStream = Tasks.await(Wearable.getDataClient(context).getFdForAsset(weatherAsset)).getInputStream()) {
                        final Weather weatherData = AsyncTask.await(new Callable<Weather>() {
                            @Override
                            public Weather call() {
                                return JSONParser.deserializer(inputStream, Weather.class);
                            }
                        });

                        if (weatherData != null && weatherData.isValid()) {
                            Settings.saveWeatherAlerts(homeData, weatherData.getWeatherAlerts());
                            Settings.saveWeatherData(weatherData);
                            Settings.saveWeatherForecasts(new Forecasts(weatherData.getQuery(), weatherData.getForecast(), weatherData.getTxtForecast()));
                            Settings.saveWeatherForecasts(weatherData.getQuery(), weatherData.getHrForecast() == null ? null :
                                    Collections2.transform(weatherData.getHrForecast(), new Function<HourlyForecast, HourlyForecasts>() {
                                        @NonNull
                                        @Override
                                        public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                            return new HourlyForecasts(weatherData.getQuery(), input);
                                        }
                                    }));
                            Settings.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(updateTimeMillis), ZoneOffset.UTC));
                            setWeatherUpdateTime(context, updateTimeMillis);

                            Timber.tag("DataSyncManager").d("Updated weather data");

                            WidgetUpdaterWorker.requestWidgetUpdate(context);
                        } else {
                            Timber.tag("DataSyncManager").d("Weather data invalid");
                        }
                    } catch (ExecutionException | InterruptedException | IOException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                } else {
                    Timber.tag("DataSyncManager").d("updateWeather: weather data missing");
                }
            }

            // Send callback to receiver
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                    new Intent(WearableHelper.WeatherPath));
        }
    }

    private static long getSettingsUpdateTime(@NonNull Context context) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        return prefs.getLong("settings_updatetime", 0);
    }

    private static long getLocationDataUpdateTime(@NonNull Context context) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        return prefs.getLong("location_updatetime", 0);
    }

    private static long getWeatherUpdateTime(@NonNull Context context) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        return prefs.getLong("weather_updatetime", 0);
    }

    private static void setSettingsUpdateTime(@NonNull Context context, long value) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        prefs.edit().putLong("settings_updatetime", value).apply();
    }

    private static void setLocationDataUpdateTime(@NonNull Context context, long value) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        prefs.edit().putLong("location_updatetime", value).apply();
    }

    private static void setWeatherUpdateTime(@NonNull Context context, long value) {
        context = context.getApplicationContext();

        SharedPreferences prefs = context.getSharedPreferences("datasync", Context.MODE_PRIVATE);
        prefs.edit().putLong("weather_updatetime", value).apply();
    }
}
