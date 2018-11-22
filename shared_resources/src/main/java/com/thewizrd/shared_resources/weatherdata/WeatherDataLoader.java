package com.thewizrd.shared_resources.weatherdata;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class WeatherDataLoader {
    private WeatherLoadedListenerInterface mCallBack;
    private WeatherErrorListenerInterface mErrorCallback;

    private LocationData location = null;
    private Weather weather = null;
    private WeatherManager wm;

    private LocalBroadcastManager mLocalBroadcastManager;

    private static final String TAG = "WeatherDataLoader";

    public WeatherDataLoader() {
        mLocalBroadcastManager = LocalBroadcastManager.
                getInstance(SimpleLibrary.getInstance().getApp().getAppContext());
    }

    public WeatherDataLoader(LocationData location) {
        this();
        wm = WeatherManager.getInstance();

        this.location = location;

        if (this.location == null)
            throw new IllegalArgumentException("Location cannot be null!!");
    }

    public WeatherDataLoader(LocationData location, WeatherLoadedListenerInterface listener) {
        this(location);
        mCallBack = listener;
    }

    public WeatherDataLoader(LocationData location, WeatherLoadedListenerInterface listener, WeatherErrorListenerInterface errorListener) {
        this(location, listener);
        mErrorCallback = errorListener;
    }

    public void setWeatherLoadedListener(WeatherLoadedListenerInterface listener) {
        mCallBack = listener;
    }

    public void setWeatherErrorListener(WeatherErrorListenerInterface listener) {
        mErrorCallback = listener;
    }

    private void getWeatherData() throws WeatherException {
        new AsyncTaskEx<Void, WeatherException>().await(new CallableEx<Void, WeatherException>() {
            @Override
            public Void call() throws WeatherException {
                WeatherException wEx = null;
                boolean loadedSavedData = false;

                try {
                    weather = wm.getWeather(location);
                } catch (WeatherException weatherEx) {
                    wEx = weatherEx;
                    weather = null;
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error getting weather data");
                    weather = null;
                }

                // Load old data if available and we can't get new data
                if (weather == null) {
                    loadedSavedData = loadSavedWeatherData(true);
                } else if (weather != null) {
                    // Handle upgrades
                    if (StringUtils.isNullOrEmpty(location.getName()) || StringUtils.isNullOrEmpty(location.getTzLong())) {
                        location.setName(weather.getLocation().getName());
                        location.setTzLong(weather.getLocation().getTzLong());

                        if (SimpleLibrary.getInstance().getApp().isPhone())
                            Settings.updateLocation(location);
                        else
                            Settings.saveHomeData(location);
                    }
                    if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                        location.setLatitude(Double.valueOf(weather.getLocation().getLatitude()));
                        location.setLongitude(Double.valueOf(weather.getLocation().getLongitude()));

                        if (SimpleLibrary.getInstance().getApp().isPhone())
                            Settings.updateLocation(location);
                        else
                            Settings.saveHomeData(location);
                    }

                    saveWeatherData();
                }

                // Throw exception if we're unable to get any weather data
                if (weather == null && wEx != null) {
                    throw wEx;
                } else if (weather == null && wEx == null) {
                    throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
                } else if (weather != null && wEx != null && loadedSavedData) {
                    throw wEx;
                }
                return null;
            }
        });
    }

    public void loadWeatherData(final boolean forceRefresh) {
        if (forceRefresh) {
            try {
                getWeatherData();
            } catch (WeatherException wEx) {
                if (mErrorCallback != null)
                    mErrorCallback.onWeatherError(wEx);
            }
        } else {
            loadWeatherData();
        }

        Logger.writeLine(Log.DEBUG, "%s: Sending weather data to callback", TAG);
        Logger.writeLine(Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
                location == null ? "null" : location.toString(), weather == null ? "false" : weather.isValid());

        if (mCallBack != null)
            mCallBack.onWeatherLoaded(location, weather);
    }

    private void loadWeatherData() {
        /*
         * If unable to retrieve saved data, data is old, or units don't match
         * Refresh weather data
         */

        Logger.writeLine(Log.DEBUG, "%s: Loading weather data for %s", TAG, location == null ? "null" : location.toString());

        boolean gotData = loadSavedWeatherData();

        if (!gotData) {
            Logger.writeLine(Log.DEBUG, "%s: Saved weather data invalid for %s", TAG, location == null ? "null" : location.toString());
            Logger.writeLine(Log.DEBUG, "%s: Retrieving data from weather provider", TAG);

            try {
                if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                        || (weather == null && location != null && !location.getSource().equals(Settings.getAPI()))) {
                    // Update location query and source for new API
                    String oldKey = location.getQuery();

                    if (weather != null)
                        location.setQuery(wm.updateLocationQuery(weather));
                    else
                        location.setQuery(wm.updateLocationQuery(location));

                    location.setSource(Settings.getAPI());

                    // Update database as well
                    if (SimpleLibrary.getInstance().getApp().isPhone()) {
                        if (location.getLocationType() == LocationType.GPS) {
                            Settings.saveLastGPSLocData(location);
                            mLocalBroadcastManager.sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                        } else {
                            Settings.updateLocationWithKey(location, oldKey);
                        }

                        mLocalBroadcastManager.sendBroadcast(
                                new Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
                                        .putExtra("oldKey", oldKey)
                                        .putExtra("location", location.toJson()));
                    } else {
                        Settings.saveHomeData(location);
                    }
                }

                getWeatherData();
            } catch (WeatherException wEx) {
                if (mErrorCallback != null)
                    mErrorCallback.onWeatherError(wEx);
            }
        }
    }

    private boolean loadSavedWeatherData(boolean _override) {
        if (_override) {
            // Load weather data
            try {
                weather = Settings.getWeatherData(location.getQuery());

                if (wm.supportsAlerts())
                    weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));
            } catch (Exception ex) {
                weather = null;
                Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data");
            }

            ULocale currentLocale = ULocale.forLocale(Locale.getDefault());
            String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

            boolean isInvalid = weather == null || !weather.isValid() || !weather.getSource().equals(Settings.getAPI());
            if (wm.supportsWeatherLocale() && !isInvalid)
                isInvalid = !weather.getLocale().equals(locale);

            return !isInvalid;
        } else
            return loadSavedWeatherData();
    }

    private boolean loadSavedWeatherData() {
        // Load weather data
        try {
            weather = Settings.getWeatherData(location.getQuery());

            if (wm.supportsAlerts())
                weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));
        } catch (Exception ex) {
            weather = null;
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data");
        }

        ULocale currentLocale = ULocale.forLocale(Locale.getDefault());
        String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

        boolean isInvalid = weather == null || !weather.isValid() || !weather.getSource().equals(Settings.getAPI());
        if (wm.supportsWeatherLocale() && !isInvalid)
            isInvalid = !weather.getLocale().equals(locale);

        if (isInvalid) return false;

        int ttl = Settings.DEFAULTINTERVAL;
        try {
            ttl = Integer.parseInt(weather.getTtl());
        } catch (NumberFormatException ex) {
            Logger.writeLine(Log.ERROR, ex);
        }

        // Check file age
        ZonedDateTime updateTime = weather.getUpdateTime();

        Duration span = Duration.between(ZonedDateTime.now(), updateTime).abs();
        return span.toMinutes() < ttl;
    }

    private void saveWeatherData() {
        // Save location query
        weather.setQuery(location.getQuery());

        // Save weather alerts
        saveWeatherAlerts();

        Settings.saveWeatherData(weather);

        if (SimpleLibrary.getInstance().getApp().isPhone()) {
            // Update weather data for Wearables
            mLocalBroadcastManager.sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));

            // Update cached weather data for widgets
            mLocalBroadcastManager.sendBroadcast(
                    new Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER)
                            .putExtra("locationQuery", location.getQuery())
                            .putExtra("weather", weather.toJson()));
        } else {
            Settings.setUpdateTime(LocalDateTime.ofInstant(weather.getUpdateTime().toInstant(), ZoneOffset.UTC));
        }
    }

    // TODO: make async
    private void saveWeatherAlerts() {
        if (weather.getWeatherAlerts() != null) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Check for previously saved alerts
                    List<WeatherAlert> previousAlerts = Settings.getWeatherAlertData(location.getQuery());

                    if (previousAlerts != null && previousAlerts.size() > 0) {
                        // If any previous alerts were flagged before as notified
                        // make sure to set them here as such
                        // bc notified flag gets reset when retrieving weatherdata
                        for (WeatherAlert alert : weather.getWeatherAlerts()) {
                            for (WeatherAlert prevAlert : previousAlerts) {
                                if (prevAlert.equals(alert) && prevAlert.isNotified()) {
                                    alert.setNotified(prevAlert.isNotified());
                                    break;
                                }
                            }
                        }
                    }

                    Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                    return null;
                }
            });
        }
    }

    public void forceLoadSavedWeatherData() {
        loadSavedWeatherData(true);

        Logger.writeLine(Log.DEBUG, "%s: Sending weather data to callback", TAG);
        Logger.writeLine(Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
                location == null ? "null" : location.toString(), weather == null ? "false" : weather.isValid());

        if (weather != null && mCallBack != null)
            mCallBack.onWeatherLoaded(location, weather);
        else if (weather == null && mErrorCallback != null)
            mErrorCallback.onWeatherError(new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER));
    }

    public Weather getWeather() {
        return weather;
    }
}
