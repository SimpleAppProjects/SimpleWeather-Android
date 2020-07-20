package com.thewizrd.simpleweather.wearable;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.LaunchActivity;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherComplicationService extends ComplicationProviderService {
    private static final String TAG = "WeatherComplicationService";

    private Context mContext;

    private WeatherManager wm;

    private FusedLocationProviderClient mFusedLocationClient;

    private static LocalDateTime updateTime = DateTimeUtils.getLocalDateTimeMIN();

    public static LocalDateTime getUpdateTime() {
        return updateTime;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        wm = WeatherManager.getInstance();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
        }
    }

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
        super.onComplicationActivated(complicationId, type, manager);

        // Request complication update
        WeatherComplicationIntentService.enqueueWork(mContext,
                new Intent(mContext, WeatherComplicationIntentService.class)
                        .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATION)
                        .putExtra(WeatherComplicationIntentService.EXTRA_COMPLICATIONID, complicationId));
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        super.onComplicationDeactivated(complicationId);
    }

    @Override
    public void onComplicationUpdate(final int complicationId, final int type, final ComplicationManager manager) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                ComplicationData complicationData = null;

                if (Settings.isWeatherLoaded()) {
                    complicationData = buildUpdate(type, getWeather());
                }

                if (complicationData != null) {
                    manager.updateComplicationData(complicationId, complicationData);
                    updateTime = LocalDateTime.now(ZoneOffset.UTC);
                    Logger.writeLine(Log.DEBUG, "%s: Complication %d updated", TAG, complicationId);
                } else {
                    // If no data is sent, we still need to inform the ComplicationManager, so
                    // the update job can finish and the wake lock isn't held any longer.
                    manager.noUpdateRequired(complicationId);
                    Logger.writeLine(Log.DEBUG, "%s: Complication %d no update required", TAG, complicationId);
                }
            }
        });
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean result = super.onUnbind(intent);
        Logger.writeLine(Log.DEBUG, "%s: Service unbound", TAG);
        return result;
    }

    private PendingIntent getTapIntent(Context context) {
        Intent onClickIntent = new Intent(context.getApplicationContext(), LaunchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, onClickIntent, 0);
    }

    private ComplicationData buildUpdate(int dataType, Weather weather) {
        if ((weather == null || !weather.isValid()) || (dataType != ComplicationData.TYPE_SHORT_TEXT && dataType != ComplicationData.TYPE_LONG_TEXT)) {
            return null;
        } else {
            // Temperature
            String temp = String.format(Locale.getDefault(), "%d°",
                    Settings.isFahrenheit() ?
                            Math.round(weather.getCondition().getTempF()) :
                            Math.round(weather.getCondition().getTempC()));
            // Weather Icon
            int weatherIcon = WeatherUtils.getWeatherIconResource(weather.getCondition().getIcon());
            // Condition text
            String condition = weather.getCondition().getWeather();

            ComplicationData.Builder builder = new ComplicationData.Builder(dataType);
            if (dataType == ComplicationData.TYPE_SHORT_TEXT) {
                builder.setShortText(ComplicationText.plainText(temp));
            } else if (dataType == ComplicationData.TYPE_LONG_TEXT) {
                builder.setLongText(ComplicationText.plainText(String.format("%s - %s", condition, temp)));
            }

            builder.setIcon(Icon.createWithResource(this, weatherIcon))
                    .setTapAction(getTapIntent(this));

            return builder.build();
        }
    }

    private Weather getWeather() {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                Weather weather = null;

                try {
                    if (Settings.getDataSync() == WearableDataSync.OFF && Settings.useFollowGPS())
                        updateLocation();

                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());

                    WeatherRequest.Builder request = new WeatherRequest.Builder();
                    if (Settings.getDataSync() == WearableDataSync.OFF) {
                        request.forceRefresh(false);
                    } else {
                        request.forceLoadSavedData();
                    }

                    weather = Tasks.await(wloader.loadWeatherData(request.build()));

                    if (weather != null && Settings.getDataSync() != WearableDataSync.OFF) {
                        int ttl = Math.max(weather.getTtl(), Settings.getRefreshInterval());

                        // Check file age
                        ZonedDateTime updateTime = Settings.getUpdateTime().atZone(ZoneOffset.UTC);

                        Duration span = Duration.between(ZonedDateTime.now(), updateTime).abs();
                        if (span.toMinutes() > ttl) {
                            WearableDataListenerService.enqueueWork(mContext, new Intent(mContext, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE));
                        }
                    }
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG);
                    return null;
                }

                return weather;
            }
        });
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    LocationManager locMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        boolean disable = true;

                        PowerManager pwrMan = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                        // Some devices (ex. Pixel) disable location services if device is in Battery Saver mode
                        // and the screen is off; don't disable this feature for this case
                        boolean lowPwrMode = pwrMan != null && (pwrMan.isPowerSaveMode() && !pwrMan.isInteractive());

                        // Disable if we're unable to access location
                        // and we're not in Low Power (Battery Saver) mode
                        disable = !lowPwrMode;

                        if (disable) {
                            // Disable GPS feature if location is not enabled
                            Settings.setFollowGPS(false);
                            Logger.writeLine(Log.INFO, "%s: Disabled location feature", TAG);
                        }

                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 10, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });
                    } else {
                        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                        if (isGPSEnabled || isNetEnabled) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);
                        }
                    }

                    if (location != null) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel query_vm = null;

                        // TODO: task it
                        try {
                            query_vm = wm.getLocation(location);
                        } catch (WeatherException e) {
                            Logger.writeLine(Log.ERROR, e);
                            return false;
                        }

                        if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                            query_vm = new LocationQueryViewModel();
                        // END TASK IT

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }
}
