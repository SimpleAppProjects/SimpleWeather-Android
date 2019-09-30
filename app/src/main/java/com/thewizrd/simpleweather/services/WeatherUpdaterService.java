package com.thewizrd.simpleweather.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver;
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherUpdaterService extends JobIntentService {
    private static String TAG = "WeatherUpdaterService";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    private static final int JOB_ID = 1004;

    private Context mContext;
    private static boolean mAlarmStarted = false;

    private WeatherManager wm;

    private FusedLocationProviderClient mFusedLocationClient;
    private CancellationTokenSource cts;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherUpdaterService.class,
                JOB_ID, work);
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        wm = WeatherManager.getInstance();

        // Check if alarm is already set
        mAlarmStarted = (PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, WeatherUpdaterService.class)
                        .setAction(ACTION_UPDATEWEATHER),
                PendingIntent.FLAG_NO_CREATE) != null);

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Logger.writeLine(Log.ERROR, e, "%s: Unhandled Exception %s", TAG, e.getMessage());

                if (oldHandler != null) {
                    oldHandler.uncaughtException(t, e);
                } else {
                    System.exit(2);
                }
            }
        });

        cts = new CancellationTokenSource();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
        }
    }

    @Override
    public void onDestroy() {
        if (Settings.showOngoingNotification() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sendBroadcast(new Intent(mContext, WeatherNotificationBroadcastReceiver.class)
                    .setAction(WeatherNotificationService.ACTION_STOPREFRESH));
        }

        Logger.writeLine(Log.INFO, "%s: destroying service and cancelling tasks...", TAG);

        try {
            if (cts != null) cts.cancel();
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "%s: Error cancelling task...", TAG);
        }

        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_STARTALARM.equals(intent.getAction())) {
            // Start alarm if it hasn't started already
            startAlarm(mContext);
        } else if (ACTION_CANCELALARM.equals(intent.getAction())) {
            // Cancel all alarms if no widgets exist
            cancelAlarms(mContext);
        } else if (ACTION_UPDATEALARM.equals(intent.getAction())) {
            // Refresh interval was changed
            // Update alarm
            updateAlarm(mContext);
        } else if (ACTION_UPDATEWEATHER.equals(intent.getAction())) {
            if (Settings.isWeatherLoaded()) {
                // Send broadcast to signal update
                if (WeatherWidgetService.widgetsExist(mContext))
                    sendBroadcast(new Intent(WeatherWidgetProvider.ACTION_SHOWREFRESH));
                // NOTE: Don't try to show refresh for pre-M devices
                // If app gets killed, instance of notif is lost & view is reset
                // and might get stuck
                if (Settings.showOngoingNotification() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    sendBroadcast(new Intent(mContext, WeatherNotificationBroadcastReceiver.class)
                            .setAction(WeatherNotificationService.ACTION_SHOWREFRESH));

                // Update for home
                final Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                    @Override
                    public Weather call() throws Exception {
                        return getWeather();
                    }
                });

                if (WeatherWidgetService.widgetsExist(mContext)) {
                    sendBroadcast(new Intent(mContext, WeatherWidgetBroadcastReceiver.class)
                            .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET));
                }

                if (weather != null) {
                    if (Settings.showOngoingNotification()) {
                        sendBroadcast(new Intent(mContext, WeatherNotificationBroadcastReceiver.class)
                                .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));
                    }

                    if (Settings.useAlerts() && wm.supportsAlerts()) {
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.getWeatherAlerts());
                            }
                        });
                    }
                }
            }
        }
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, WeatherUpdaterService.class)
                .setAction(ACTION_UPDATEWEATHER);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static void updateAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int interval = Settings.getRefreshInterval();

        boolean startNow = !mAlarmStarted;
        long intervalMillis = Duration.ofMinutes(interval).toMillis();
        long triggerAtTime = SystemClock.elapsedRealtime() + intervalMillis;

        if (startNow) {
            enqueueWork(context, new Intent(context, WeatherUpdaterService.class)
                    .setAction(ACTION_UPDATEWEATHER));
        }

        PendingIntent pendingIntent = getAlarmIntent(context);
        am.cancel(pendingIntent);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, intervalMillis, pendingIntent);
        mAlarmStarted = true;

        Logger.writeLine(Log.INFO, "%s: Updated alarm", TAG);
    }

    private void cancelAlarms(Context context) {
        // Cancel alarm if dependent features are turned off
        if (!WeatherWidgetService.widgetsExist(context) && !Settings.showOngoingNotification() && !Settings.useAlerts()) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(getAlarmIntent(context));
            mAlarmStarted = false;

            Logger.writeLine(Log.INFO, "%s: Canceled alarm", TAG);
        }
    }

    private void startAlarm(Context context) {
        // Start alarm if dependent features are enabled
        if (!mAlarmStarted && (WeatherWidgetService.widgetsExist(context) || Settings.showOngoingNotification() || Settings.useAlerts())) {
            updateAlarm(context);
            mAlarmStarted = true;
        }
    }

    private Weather getWeather() {
        return getWeather(true);
    }

    private Weather getWeather(final boolean refreshWeather) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                Weather weather = null;

                try {
                    if (Settings.useFollowGPS())
                        updateLocation();

                    if (isCtsCancelRequested()) throw new InterruptedException();

                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());
                    if (refreshWeather)
                        wloader.loadWeatherData(false);
                    else
                        wloader.forceLoadSavedWeatherData();

                    weather = wloader.getWeather();

                    if (refreshWeather && weather != null) {
                        // Re-schedule alarm at selected interval from now
                        updateAlarm(App.getInstance().getAppContext());
                        Settings.setUpdateTime(LocalDateTime.now());
                    }
                } catch (InterruptedException cancelEx) {
                    Logger.writeLine(Log.ERROR, cancelEx, "%s: GetWeather cancelled", TAG);
                    return null;
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
            public Boolean call() throws Exception {
                boolean locationChanged = false;

                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    LocationManager locMan = (LocationManager) App.getInstance().getAppContext().getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        // Disable GPS feature if location is not enabled
                        Settings.setFollowGPS(false);
                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() throws Exception {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
                                } catch (TimeoutException e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });
                    } else {
                        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                        if (isGPSEnabled || isNetEnabled && !isCtsCancelRequested()) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);
                        }
                    }

                    if (location != null && !isCtsCancelRequested()) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        if (isCtsCancelRequested()) return locationChanged;

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel query_vm = null;

                        TaskCompletionSource<LocationQueryViewModel> tcs = new TaskCompletionSource<>(cts.getToken());
                        tcs.setResult(wm.getLocation(location));
                        try {
                            query_vm = Tasks.await(tcs.getTask());
                        } catch (ExecutionException e) {
                            query_vm = new LocationQueryViewModel();
                            Logger.writeLine(Log.ERROR, e.getCause());
                        } catch (InterruptedException e) {
                            return locationChanged;
                        }

                        if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                            query_vm = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested()) return locationChanged;

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        LocalBroadcastManager.getInstance(App.getInstance().getAppContext())
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));

                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }
}
