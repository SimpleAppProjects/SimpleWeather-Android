package com.thewizrd.shared_resources.weatherdata;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.TaskUtils;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.LocationUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public final class WeatherDataLoader {
    private final LocationData location;
    private Weather weather = null;
    private Collection<WeatherAlert> weatherAlerts = null;
    private final WeatherManager wm = WeatherManager.getInstance();

    private final LocalBroadcastManager mLocalBroadcastManager;

    private static final String TAG = "WeatherDataLoader";

    public WeatherDataLoader(@NonNull LocationData location) {
        this.location = location;

        mLocalBroadcastManager = LocalBroadcastManager.
                getInstance(SimpleLibrary.getInstance().getApp().getAppContext());
    }

    public Task<Weather> loadWeatherData(@NonNull final WeatherRequest request) {
        final TaskCompletionSource<Weather> tcs;
        if (request.getCancellationToken() != null) {
            tcs = new TaskCompletionSource<>(request.getCancellationToken());
        } else {
            tcs = new TaskCompletionSource<>();
        }

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                try {
                    tcs.setResult(getCallable(request).call().getWeather());
                } catch (Exception e) {
                    tcs.setException(e);
                }
            }
        });

        return tcs.getTask();
    }

    public Task<WeatherResult> loadWeatherResult(@NonNull final WeatherRequest request) {
        return AsyncTask.create(getCallable(request));
    }

    private Callable<WeatherResult> getCallable(@NonNull final WeatherRequest request) {
        return new Callable<WeatherResult>() {
            @Override
            @NonNull
            public WeatherResult call() throws WeatherException {
                WeatherResult result = null;

                try {
                    if (request.isForceLoadSavedData()) {
                        loadSavedWeatherData(request, true);
                    } else {
                        if (request.isForceRefresh()) {
                            result = getWeatherData(request);
                        } else {
                            if (!isDataValid(false)) {
                                result = _loadWeatherData(request);
                            }
                        }
                    }
                    if (request.isShouldSaveData()) {
                        checkForOutdatedObservation();
                    }
                } catch (WeatherException wEx) {
                    if (request.getErrorListener() != null) {
                        request.getErrorListener().onWeatherError(wEx);
                    } else {
                        throw wEx;
                    }
                }

                Logger.writeLine(Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
                        location == null ? "null" : location.toString(), weather == null ? "false" : weather.isValid());

                if (result == null) {
                    result = WeatherResult.create(weather, false);
                }

                return result;
            }
        };
    }

    public Task<Collection<WeatherAlert>> loadWeatherAlerts(final boolean loadSavedData) {
        return AsyncTask.create(new Callable<Collection<WeatherAlert>>() {
            @Override
            public Collection<WeatherAlert> call() {
                if (wm.supportsAlerts()) {
                    if (wm.needsExternalAlertData()) {
                        if (!loadSavedData) {
                            weatherAlerts = wm.getAlerts(location);
                        }
                    }

                    if (weatherAlerts == null) {
                        weatherAlerts = Settings.getWeatherAlertData(location.getQuery());
                    }

                    if (!loadSavedData) {
                        saveWeatherAlerts();
                    }
                }

                return weatherAlerts;
            }
        });
    }

    @CanIgnoreReturnValue
    private WeatherResult getWeatherData(final WeatherRequest request) throws WeatherException {
        WeatherException wEx = null;
        boolean loadedSavedData = false;

        // Try to get weather from provider API
        try {
            TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

            if (WeatherAPI.NWS.equals(Settings.getAPI()) && !LocationUtils.isUS(location.getCountryCode())) {
                // If location data hasn't been updated, try loading weather from the previous provider
                if (!StringUtils.isNullOrWhitespace(location.getWeatherSource()) &&
                        !WeatherAPI.NWS.equals(location.getWeatherSource())) {
                    weather = WeatherManager.getProvider(location.getWeatherSource()).getWeather(location);
                } else {
                    throw new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
                }
            } else {
                // Load weather from provider
                weather = wm.getWeather(location);
            }
        } catch (WeatherException weatherEx) {
            wEx = weatherEx;
            weather = null;
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error getting weather data");
            weather = null;
        }

        if (request.isLoadAlerts() && weather != null && wm.supportsAlerts() && wm.needsExternalAlertData()) {
            weather.setWeatherAlerts(wm.getAlerts(location));

            if (weather.getWeatherAlerts() == null) {
                weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));
            }
        }

        if (request.isShouldSaveData()) {
            // Load old data if available and we can't get new data
            if (weather == null) {
                loadedSavedData = loadSavedWeatherData(request, true);
            } else {
                // Handle upgrades
                if (StringUtils.isNullOrEmpty(location.getName()) || StringUtils.isNullOrEmpty(location.getTzLong())) {
                    location.setName(weather.getLocation().getName());
                    location.setTzLong(weather.getLocation().getTzLong());

                    if (SimpleLibrary.getInstance().getApp().isPhone())
                        Settings.updateLocation(location);
                    else
                        Settings.saveHomeData(location);
                }
                if (location.getLatitude() == 0 && location.getLongitude() == 0 &&
                        weather.getLocation().getLatitude() != null && weather.getLocation().getLongitude() != null) {
                    location.setLatitude(weather.getLocation().getLatitude());
                    location.setLongitude(weather.getLocation().getLongitude());

                    if (SimpleLibrary.getInstance().getApp().isPhone())
                        Settings.updateLocation(location);
                    else
                        Settings.saveHomeData(location);
                }
                if (StringUtils.isNullOrWhitespace(location.getLocationSource())) {
                    location.setLocationSource(wm.getLocationProvider().getLocationAPI());

                    if (SimpleLibrary.getInstance().getApp().isPhone())
                        Settings.updateLocation(location);
                    else
                        Settings.saveHomeData(location);
                }

                saveWeatherData();
                saveWeatherForecasts();

                if ((request.isLoadAlerts() || weather.getWeatherAlerts() != null) && wm.supportsAlerts() && !wm.needsExternalAlertData()) {
                    weatherAlerts = weather.getWeatherAlerts();
                    saveWeatherAlerts();
                }
            }
        }

        // Throw exception if we're unable to get any weather data
        if (weather == null && wEx != null) {
            throw wEx;
        } else if (weather == null && wEx == null) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        } else if (weather != null && wEx != null && loadedSavedData) {
            throw wEx;
        }

        return WeatherResult.create(weather, !loadedSavedData);
    }

    @CanIgnoreReturnValue
    private WeatherResult _loadWeatherData(final WeatherRequest request) throws WeatherException {
        /*
         * If unable to retrieve saved data, data is old, or units don't match
         * Refresh weather data
         */

        Logger.writeLine(Log.DEBUG, "%s: Loading weather data for %s", TAG, location == null ? "null" : location.toString());

        boolean gotData = loadSavedWeatherData(request);

        if (!gotData) {
            if (request.isShouldSaveData()) {
                Logger.writeLine(Log.DEBUG, "%s: Saved weather data invalid for %s", TAG, location == null ? "null" : location.toString());
                Logger.writeLine(Log.DEBUG, "%s: Retrieving data from weather provider", TAG);

                if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                        || (weather == null && location != null && !location.getWeatherSource().equals(Settings.getAPI()))) {
                    // Only update location data if weather provider is not NWS or if it is NWS and the location is supported
                    if (!WeatherAPI.NWS.equals(Settings.getAPI()) || LocationUtils.isUS(location.getCountryCode())) {
                        // Update location query and source for new API
                        String oldKey = location.getQuery();

                        if (weather != null)
                            location.setQuery(wm.updateLocationQuery(weather));
                        else
                            location.setQuery(wm.updateLocationQuery(location));

                        location.setWeatherSource(Settings.getAPI());

                        // Update database as well
                        if (SimpleLibrary.getInstance().getApp().isPhone()) {
                            if (location.getLocationType() == LocationType.GPS) {
                                Settings.saveLastGPSLocData(location);
                                mLocalBroadcastManager.sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                            } else {
                                Settings.updateLocationWithKey(location, oldKey);
                                mLocalBroadcastManager.sendBroadcast(
                                        new Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
                                                .putExtra(Constants.WIDGETKEY_OLDKEY, oldKey)
                                                .putExtra(Constants.WIDGETKEY_LOCATION, JSONParser.serializer(location, LocationData.class)));
                            }
                        } else {
                            Settings.saveHomeData(location);
                        }
                    }
                }
            }

            return getWeatherData(request);
        } else {
            return WeatherResult.create(weather, false);
        }
    }

    private boolean loadSavedWeatherData(WeatherRequest request) throws WeatherException {
        return loadSavedWeatherData(request, false);
    }

    private boolean loadSavedWeatherData(WeatherRequest request, boolean _override) throws WeatherException {
        // Load weather data
        try {
            TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

            weather = Settings.getWeatherData(location.getQuery());

            if (request.isLoadAlerts() && weather != null && wm.supportsAlerts())
                weather.setWeatherAlerts(weatherAlerts = Settings.getWeatherAlertData(location.getQuery()));

            TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

            if (request.isLoadForecasts() && weather != null) {
                Forecasts forecasts = Settings.getWeatherForecastData(location.getQuery());
                List<HourlyForecast> hrForecasts = Settings.getHourlyWeatherForecastData(location.getQuery());
                if (forecasts != null) {
                    weather.setForecast(forecasts.getForecast());
                    weather.setTxtForecast(forecasts.getTxtForecast());
                }
                weather.setHrForecast(hrForecasts);
            }

            TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

            if (_override && weather == null) {
                // If weather is still unavailable try manually searching for it
                weather = Settings.getWeatherDataByCoordinate(location);

                TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

                if (request.isLoadAlerts() && weather != null && wm.supportsAlerts())
                    weather.setWeatherAlerts(weatherAlerts = Settings.getWeatherAlertData(location.getQuery()));

                TaskUtils.throwIfCancellationRequested(request.getCancellationToken());

                if (request.isLoadForecasts() && weather != null) {
                    Forecasts forecasts = Settings.getWeatherForecastData(location.getQuery());
                    List<HourlyForecast> hrForecasts = Settings.getHourlyWeatherForecastData(location.getQuery());
                    if (forecasts != null) {
                        weather.setForecast(forecasts.getForecast());
                        weather.setTxtForecast(forecasts.getTxtForecast());
                    }
                    weather.setHrForecast(hrForecasts);
                }
            }
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data");
            weather = null;
            throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        }

        return isDataValid(_override);
    }

    private void checkForOutdatedObservation() {
        if (weather != null) {
            // Check for outdated observation
            final ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(location.getTzOffset());
            long duraMins = weather.getCondition().getObservationTime() == null ? 61 : Duration.between(weather.getCondition().getObservationTime(), now).toMinutes();
            if (duraMins > 60) {
                HourlyForecast hrf = Settings.getFirstHourlyForecastDataByDate(location.getQuery(), now.truncatedTo(ChronoUnit.HOURS));
                if (hrf != null) {
                    weather.getCondition().setWeather(hrf.getCondition());
                    weather.getCondition().setIcon(hrf.getIcon());

                    weather.getCondition().setTempF(hrf.getHighF() != null ? hrf.getHighF() : null);
                    weather.getCondition().setTempC(hrf.getHighC() != null ? hrf.getHighC() : null);

                    weather.getCondition().setWindMph(hrf.getWindMph() != null ? hrf.getWindMph() : null);
                    weather.getCondition().setWindKph(hrf.getWindKph() != null ? hrf.getWindKph() : null);
                    weather.getCondition().setWindDegrees(hrf.getWindDegrees() != null ? hrf.getWindDegrees() : null);

                    if (hrf.getWindMph() != null) {
                        weather.getCondition().setBeaufort(new Beaufort(WeatherUtils.getBeaufortScale(Math.round(hrf.getWindMph())).getValue()));
                    }
                    weather.getCondition().setFeelslikeF(hrf.getExtras() != null && hrf.getExtras().getFeelslikeF() != null ? hrf.getExtras().getFeelslikeF() : null);
                    weather.getCondition().setFeelslikeC(hrf.getExtras() != null && hrf.getExtras().getFeelslikeC() != null ? hrf.getExtras().getFeelslikeC() : null);
                    weather.getCondition().setUv(hrf.getExtras() != null && hrf.getExtras().getUvIndex() != null && hrf.getExtras().getUvIndex() >= 0 ? new UV(hrf.getExtras().getUvIndex()) : null);

                    weather.getCondition().setObservationTime(hrf.getDate());

                    if (duraMins > 60 * 6 || (weather.getCondition().getHighF() == null || ObjectsCompat.equals(weather.getCondition().getHighF(), weather.getCondition().getLowF()))) {
                        Forecasts fcasts = Settings.getWeatherForecastData(location.getQuery());
                        Forecast fcast = null;

                        if (fcasts != null && fcasts.getForecast() != null) {
                            fcast = Iterables.find(fcasts.getForecast(), new Predicate<Forecast>() {
                                @Override
                                public boolean apply(@NullableDecl Forecast input) {
                                    return input != null && input.getDate().toLocalDate().isEqual(now.toLocalDate());
                                }
                            }, null);
                        }

                        if (fcast != null) {
                            weather.getCondition().setHighF(fcast.getHighF());
                            weather.getCondition().setHighC(fcast.getHighC());
                            weather.getCondition().setLowF(fcast.getLowF());
                            weather.getCondition().setLowC(fcast.getLowC());
                        } else {
                            weather.getCondition().setHighF(0f);
                            weather.getCondition().setHighC(0f);
                            weather.getCondition().setLowF(0f);
                            weather.getCondition().setLowC(0f);
                        }
                    }

                    weather.getAtmosphere().setDewpointF(hrf.getExtras() != null && hrf.getExtras().getDewpointF() != null ? hrf.getExtras().getDewpointF() : null);
                    weather.getAtmosphere().setDewpointC(hrf.getExtras() != null && hrf.getExtras().getDewpointC() != null ? hrf.getExtras().getDewpointC() : null);
                    weather.getAtmosphere().setHumidity(hrf.getExtras() != null && hrf.getExtras().getHumidity() != null ? hrf.getExtras().getHumidity() : null);
                    weather.getAtmosphere().setPressureTrend(null);
                    weather.getAtmosphere().setPressureIn(hrf.getExtras() != null && hrf.getExtras().getPressureIn() != null ? hrf.getExtras().getPressureIn() : null);
                    weather.getAtmosphere().setPressureMb(hrf.getExtras() != null && hrf.getExtras().getPressureMb() != null ? hrf.getExtras().getPressureMb() : null);
                    weather.getAtmosphere().setVisibilityMi(hrf.getExtras() != null && hrf.getExtras().getVisibilityMi() != null ? hrf.getExtras().getVisibilityMi() : null);
                    weather.getAtmosphere().setVisibilityKm(hrf.getExtras() != null && hrf.getExtras().getVisibilityKm() != null ? hrf.getExtras().getVisibilityKm() : null);

                    if (weather.getPrecipitation() != null) {
                        weather.getPrecipitation().setPop(hrf.getExtras() != null && hrf.getExtras().getPop() != null ? hrf.getExtras().getPop() : null);
                        weather.getPrecipitation().setCloudiness(hrf.getExtras() != null && hrf.getExtras().getCloudiness() != null ? hrf.getExtras().getCloudiness() : null);
                        weather.getPrecipitation().setQpfRainIn(hrf.getExtras() != null && hrf.getExtras().getQpfRainIn() != null && hrf.getExtras().getQpfRainIn() >= 0 ? hrf.getExtras().getQpfRainIn() : 0.0f);
                        weather.getPrecipitation().setQpfRainMm(hrf.getExtras() != null && hrf.getExtras().getQpfRainMm() != null && hrf.getExtras().getQpfRainMm() >= 0 ? hrf.getExtras().getQpfRainMm() : 0.0f);
                        weather.getPrecipitation().setQpfSnowIn(hrf.getExtras() != null && hrf.getExtras().getQpfSnowIn() != null && hrf.getExtras().getQpfSnowIn() >= 0 ? hrf.getExtras().getQpfSnowIn() : 0.0f);
                        weather.getPrecipitation().setQpfSnowCm(hrf.getExtras() != null && hrf.getExtras().getQpfSnowCm() != null && hrf.getExtras().getQpfSnowCm() >= 0 ? hrf.getExtras().getQpfSnowCm() : 0.0f);
                    }

                    Settings.saveWeatherData(weather);
                }
            }

            // Check for outdated forecasts
            if (weather.getForecast() != null && !weather.getForecast().isEmpty()) {
                Iterables.removeIf(weather.getForecast(), new Predicate<Forecast>() {
                    @Override
                    public boolean apply(@NullableDecl Forecast input) {
                        return input == null || input.getDate().truncatedTo(ChronoUnit.DAYS).isBefore(now.toLocalDateTime().truncatedTo(ChronoUnit.DAYS));
                    }
                });
            }

            if (weather.getHrForecast() != null && !weather.getHrForecast().isEmpty()) {
                Iterables.removeIf(weather.getHrForecast(), new Predicate<HourlyForecast>() {
                    @Override
                    public boolean apply(@NullableDecl HourlyForecast input) {
                        return input == null || input.getDate().truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS));
                    }
                });
            }
        }
    }

    private boolean isDataValid(boolean _override) {
        ULocale currentLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

        String API = Settings.getAPI();
        boolean isInvalid = weather == null || !weather.isValid();
        if (!isInvalid && !weather.getSource().equals(API)) {
            if (!API.equals(WeatherAPI.NWS) || LocationUtils.isUS(location.getCountryCode())) {
                isInvalid = true;
            }
        }

        if (wm.supportsWeatherLocale() && !isInvalid)
            isInvalid = !weather.getLocale().equals(locale);

        if (_override || isInvalid) return !isInvalid;

        int ttl = Math.max(weather.getTtl(), Settings.getRefreshInterval());

        // Check file age
        ZonedDateTime updateTime = weather.getUpdateTime();

        Duration span = Duration.between(ZonedDateTime.now(), updateTime).abs();
        return span.toMinutes() < ttl;
    }

    private void saveWeatherData() {
        // Save location query
        weather.setQuery(location.getQuery());

        Settings.saveWeatherData(weather);

        if (SimpleLibrary.getInstance().getApp().isPhone()) {
            // Update cached weather data for widgets
            mLocalBroadcastManager.sendBroadcast(
                    new Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER)
                            .putExtra(Constants.WIDGETKEY_LOCATIONQUERY,
                                    location.getLocationType() == LocationType.GPS ? Constants.KEY_GPS : location.getQuery())
                            .putExtra(Constants.WIDGETKEY_WEATHER, JSONParser.serializer(weather, Weather.class)));
        } else {
            Settings.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
    }

    private void saveWeatherAlerts() {
        if (weatherAlerts != null) {
            AsyncTask.await(new Callable<Void>() {
                @Override
                public Void call() {
                    // Check for previously saved alerts
                    Collection<WeatherAlert> previousAlerts = Settings.getWeatherAlertData(location.getQuery());

                    if (previousAlerts.size() > 0) {
                        // If any previous alerts were flagged before as notified
                        // make sure to set them here as such
                        // bc notified flag gets reset when retrieving weatherdata
                        for (WeatherAlert alert : weatherAlerts) {
                            for (WeatherAlert prevAlert : previousAlerts) {
                                if (prevAlert.equals(alert) && prevAlert.isNotified()) {
                                    alert.setNotified(prevAlert.isNotified());
                                    break;
                                }
                            }
                        }
                    }

                    Settings.saveWeatherAlerts(location, weatherAlerts);
                    return null;
                }
            });
        }
    }

    private void saveWeatherForecasts() {
        AsyncTask.await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Forecasts forecasts = new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast());
                Settings.saveWeatherForecasts(forecasts);
                ArrayList<HourlyForecasts> hrForecasts = new ArrayList<>();
                if (weather.getHrForecast() != null) {
                    hrForecasts.ensureCapacity(weather.getHrForecast().size());
                    for (HourlyForecast f : weather.getHrForecast()) {
                        hrForecasts.add(new HourlyForecasts(weather.getQuery(), f));
                    }
                }
                Settings.saveWeatherForecasts(location.getQuery(), hrForecasts);
                return null;
            }
        });
    }
}
