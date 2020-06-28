package com.thewizrd.shared_resources.weatherdata;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class WeatherDataLoader {
    private LocationData location = null;
    private Weather weather = null;
    private WeatherManager wm;

    private LocalBroadcastManager mLocalBroadcastManager;

    private static final String TAG = "WeatherDataLoader";

    public WeatherDataLoader(LocationData location) {
        mLocalBroadcastManager = LocalBroadcastManager.
                getInstance(SimpleLibrary.getInstance().getApp().getAppContext());
        wm = WeatherManager.getInstance();

        this.location = location;

        if (this.location == null)
            throw new IllegalArgumentException("Location cannot be null!!");
    }

    private void getWeatherData(final WeatherRequest request) throws WeatherException {
        new AsyncTaskEx<Void, WeatherException>().await(new CallableEx<Void, WeatherException>() {
            @Override
            public Void call() throws WeatherException {
                WeatherException wEx = null;
                boolean loadedSavedData = false;

                try {
                    if (WeatherAPI.NWS.equals(Settings.getAPI()) && !"US".equals(location.getCountryCode())) {
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
                    if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                        location.setLatitude(Double.parseDouble(weather.getLocation().getLatitude()));
                        location.setLongitude(Double.parseDouble(weather.getLocation().getLongitude()));

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

    public Task<Weather> loadWeatherData(final WeatherRequest request) {
        return AsyncTask.create(new Callable<Weather>() {
            @Override
            public Weather call() throws WeatherException {
                try {
                    if (request.isForceLoadSavedData()) {
                        loadSavedWeatherData(request, true);
                    } else {
                        if (request.isForceRefresh()) {
                            getWeatherData(request);
                        } else {
                            if (!isDataValid(false)) {
                                _loadWeatherData(request);
                            }
                        }
                    }
                } catch (WeatherException wEx) {
                    if (request.getErrorListener() != null)
                        request.getErrorListener().onWeatherError(wEx);
                    else
                        throw wEx;
                }

                Logger.writeLine(Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
                        location == null ? "null" : location.toString(), weather == null ? "false" : weather.isValid());

                return weather;
            }
        });
    }

    private void _loadWeatherData(WeatherRequest request) throws WeatherException {
        /*
         * If unable to retrieve saved data, data is old, or units don't match
         * Refresh weather data
         */

        Logger.writeLine(Log.DEBUG, "%s: Loading weather data for %s", TAG, location == null ? "null" : location.toString());

        boolean gotData = loadSavedWeatherData(request);

        if (!gotData) {
            Logger.writeLine(Log.DEBUG, "%s: Saved weather data invalid for %s", TAG, location == null ? "null" : location.toString());
            Logger.writeLine(Log.DEBUG, "%s: Retrieving data from weather provider", TAG);

            if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                    || (weather == null && location != null && !location.getWeatherSource().equals(Settings.getAPI()))) {
                // Only update location data if weather provider is not NWS or if it is NWS and the location is supported
                if (!WeatherAPI.NWS.equals(location.getWeatherSource()) || "US".equals(location.getCountryCode())) {
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

            getWeatherData(request);
        }
    }

    private boolean loadSavedWeatherData(WeatherRequest request) throws WeatherException {
        return loadSavedWeatherData(request, false);
    }

    private boolean loadSavedWeatherData(WeatherRequest request, boolean _override) throws WeatherException {
        // Load weather data
        try {
            weather = Settings.getWeatherData(location.getQuery());

            if (request.isLoadAlerts() && weather != null && wm.supportsAlerts())
                weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));

            if (request.isLoadForecasts() && weather != null) {
                Forecasts forecasts = Settings.getWeatherForecastData(location.getQuery());
                List<HourlyForecast> hrForecasts = Settings.getHourlyWeatherForecastData(location.getQuery());
                weather.setForecast(forecasts.getForecast());
                weather.setHrForecast(hrForecasts);
                weather.setTxtForecast(forecasts.getTxtForecast());
            }

            if (_override && weather == null) {
                // If weather is still unavailable try manually searching for it
                weather = Settings.getWeatherDataByCoordinate(location);

                if (request.isLoadAlerts() && weather != null && wm.supportsAlerts())
                    weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));

                if (request.isLoadForecasts() && weather != null) {
                    Forecasts forecasts = Settings.getWeatherForecastData(location.getQuery());
                    List<HourlyForecast> hrForecasts = Settings.getHourlyWeatherForecastData(location.getQuery());
                    weather.setForecast(forecasts.getForecast());
                    weather.setHrForecast(hrForecasts);
                    weather.setTxtForecast(forecasts.getTxtForecast());
                }
            }

            if (weather != null) {
                // Check for outdated observation
                long duraMins = Duration.between(weather.getCondition().getObservationTime(), ZonedDateTime.now()).toMinutes();
                if (duraMins > 60) {
                    HourlyForecast hrf = Settings.getFirstHourlyForecastDataByDate(location.getQuery(), ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS));
                    if (hrf != null) {
                        weather.getCondition().setWeather(hrf.getCondition());
                        weather.getCondition().setIcon(hrf.getIcon());

                        weather.getCondition().setTempF(Double.parseDouble(hrf.getHighF()));
                        weather.getCondition().setTempC(Double.parseDouble(hrf.getHighC()));

                        weather.getCondition().setWindMph(hrf.getWindMph());
                        weather.getCondition().setWindKph(hrf.getWindKph());
                        weather.getCondition().setWindDegrees(hrf.getWindDegrees());

                        weather.getCondition().setBeaufort(new Beaufort(WeatherUtils.getBeaufortScale(Math.round(hrf.getWindMph())).getValue()));
                        weather.getCondition().setFeelslikeF(hrf.getExtras() != null ? hrf.getExtras().getFeelslikeF() : 0.0);
                        weather.getCondition().setFeelslikeC(hrf.getExtras() != null ? hrf.getExtras().getFeelslikeC() : 0.0);
                        weather.getCondition().setUv(hrf.getExtras() != null && hrf.getExtras().getUvIndex() >= 0 ? new UV(hrf.getExtras().getUvIndex()) : null);

                        weather.getCondition().setObservationTime(hrf.getDate());

                        if (duraMins > 60 * 6) {
                            weather.getCondition().setHighF(0);
                            weather.getCondition().setHighC(0);
                            weather.getCondition().setLowF(0);
                            weather.getCondition().setLowC(0);
                        }

                        weather.getAtmosphere().setDewpointF(hrf.getExtras() != null ? hrf.getExtras().getDewpointF() : null);
                        weather.getAtmosphere().setDewpointC(hrf.getExtras() != null ? hrf.getExtras().getDewpointC() : null);
                        weather.getAtmosphere().setHumidity(hrf.getExtras() != null ? hrf.getExtras().getHumidity() : null);
                        weather.getAtmosphere().setPressureTrend(null);
                        weather.getAtmosphere().setPressureIn(hrf.getExtras() != null ? hrf.getExtras().getPressureIn() : null);
                        weather.getAtmosphere().setPressureMb(hrf.getExtras() != null ? hrf.getExtras().getPressureMb() : null);
                        weather.getAtmosphere().setVisibilityMi(hrf.getExtras() != null ? hrf.getExtras().getVisibilityMi() : null);
                        weather.getAtmosphere().setVisibilityKm(hrf.getExtras() != null ? hrf.getExtras().getVisibilityKm() : null);

                        if (weather.getPrecipitation() != null) {
                            weather.getPrecipitation().setPop(hrf.getExtras() != null ? hrf.getExtras().getPop() : null);
                            weather.getPrecipitation().setQpfRainIn(hrf.getExtras() != null && hrf.getExtras().getQpfRainIn() >= 0 ? hrf.getExtras().getQpfRainIn() : 0.0f);
                            weather.getPrecipitation().setQpfRainMm(hrf.getExtras() != null && hrf.getExtras().getQpfRainMm() >= 0 ? hrf.getExtras().getQpfRainMm() : 0.0f);
                            weather.getPrecipitation().setQpfSnowIn(hrf.getExtras() != null && hrf.getExtras().getQpfSnowIn() >= 0 ? hrf.getExtras().getQpfSnowIn() : 0.0f);
                            weather.getPrecipitation().setQpfSnowCm(hrf.getExtras() != null && hrf.getExtras().getQpfSnowCm() >= 0 ? hrf.getExtras().getQpfSnowCm() : 0.0f);
                        }

                        Settings.saveWeatherData(weather);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data");
            throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        }

        return isDataValid(_override);
    }

    private boolean isDataValid(boolean _override) {
        ULocale currentLocale = ULocale.forLocale(Locale.getDefault());
        String locale = wm.localeToLangCode(currentLocale.getLanguage(), currentLocale.toLanguageTag());

        String API = Settings.getAPI();
        boolean isInvalid = weather == null || !weather.isValid();
        if (!isInvalid && !weather.getSource().equals(API)) {
            if (!API.equals(WeatherAPI.NWS) || "US".equals(location.getCountryCode())) {
                isInvalid = true;
            }
        }

        if (wm.supportsWeatherLocale() && !isInvalid)
            isInvalid = !weather.getLocale().equals(locale);

        if (_override || isInvalid) return !isInvalid;

        int ttl = Settings.DEFAULTINTERVAL;
        try {
            ttl = Integer.parseInt(weather.getTtl());
        } catch (NumberFormatException ex) {
            Logger.writeLine(Log.ERROR, ex);
        } finally {
            ttl = Math.max(ttl, Settings.getRefreshInterval());
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

        saveWeatherForecasts();

        Settings.saveWeatherData(weather);

        if (SimpleLibrary.getInstance().getApp().isPhone()) {
            // Update cached weather data for widgets
            mLocalBroadcastManager.sendBroadcast(
                    new Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER)
                            .putExtra(Constants.WIDGETKEY_LOCATIONQUERY,
                                    location.getLocationType() == LocationType.GPS ? Constants.KEY_GPS : location.getQuery())
                            .putExtra(Constants.WIDGETKEY_WEATHER, JSONParser.serializer(weather, Weather.class)));
        } else {
            Settings.setUpdateTime(LocalDateTime.ofInstant(weather.getUpdateTime().toInstant(), ZoneOffset.UTC));
        }
    }

    private void saveWeatherAlerts() {
        if (weather.getWeatherAlerts() != null) {
            new AsyncTask<Void>().await(new Callable<Void>() {
                @Override
                public Void call() {
                    // Check for previously saved alerts
                    Collection<WeatherAlert> previousAlerts = Settings.getWeatherAlertData(location.getQuery());

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

    private void saveWeatherForecasts() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Forecasts forecasts = new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast());
                Settings.saveWeatherForecasts(forecasts);
                Collection<HourlyForecasts> hrForecasts = new LinkedList<>();
                if (weather.getHrForecast() != null) {
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
