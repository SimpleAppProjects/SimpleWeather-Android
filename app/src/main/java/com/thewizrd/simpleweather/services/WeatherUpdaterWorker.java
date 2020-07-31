package com.thewizrd.simpleweather.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.LocalDateTime;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WeatherUpdaterWorker extends Worker {
    private static String TAG = "WeatherUpdaterWorker";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    private Context mContext;

    private WeatherManager wm;

    private FusedLocationProviderClient mFusedLocationClient;
    private CancellationTokenSource cts;

    public WeatherUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();

        wm = WeatherManager.getInstance();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(mContext);
        }

        cts = new CancellationTokenSource();
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        context = context.getApplicationContext();

        switch (intentAction) {
            case ACTION_UPDATEALARM:
                enqueueWork(context);
                break;
            case ACTION_UPDATEWEATHER:
            case ACTION_STARTALARM:
                // For immediate action
                startWork(context);
                break;
            case ACTION_CANCELALARM:
                cancelWork(context);
                break;
        }
    }

    private static void startWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        // Check if features are actually enabled which require this service
        if (cancelWork(context)) return;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WeatherUpdaterWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.KEEP, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);

        // Enqueue periodic task as well
        enqueueWork(context);
    }

    private static void enqueueWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, Boolean.toString(isWorkScheduled(context)));

        // Check if features are actually enabled which require this service
        if (cancelWork(context)) return;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        PeriodicWorkRequest updateRequest =
                new PeriodicWorkRequest.Builder(WeatherUpdaterWorker.class, 60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG);
    }

    private static boolean isWorkScheduled(@NonNull Context context) {
        context = context.getApplicationContext();
        WorkManager workMgr = WorkManager.getInstance(context);
        List<WorkInfo> statuses = null;
        try {
            statuses = workMgr.getWorkInfosForUniqueWork(TAG).get();
        } catch (ExecutionException | InterruptedException ignored) {
        }
        if (statuses == null || statuses.isEmpty()) return false;
        boolean running = false;
        for (WorkInfo workStatus : statuses) {
            running = workStatus.getState() == WorkInfo.State.RUNNING
                    | workStatus.getState() == WorkInfo.State.ENQUEUED;
        }
        return running;
    }


    private static boolean cancelWork(@NonNull Context context) {
        // Cancel alarm if dependent features are turned off
        context = context.getApplicationContext();
        if (!WeatherWidgetService.widgetsExist(context) && !Settings.showOngoingNotification() && !Settings.useAlerts()) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG);
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG);
            return true;
        }

        return false;
    }

    @Override
    public void onStopped() {
        if (cts != null) cts.cancel();
        super.onStopped();
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        if (Settings.isWeatherLoaded()) {
            // Update for home
            final Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                @Override
                public Weather call() {
                    return getWeather();
                }
            });

            if (WeatherWidgetService.widgetsExist(mContext)) {
                mContext.sendBroadcast(new Intent(mContext, WeatherWidgetBroadcastReceiver.class)
                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET));
            }

            if (weather != null) {
                if (Settings.showOngoingNotification()) {
                    mContext.sendBroadcast(new Intent(mContext, WeatherNotificationBroadcastReceiver.class)
                            .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION));
                }

                if (Settings.useAlerts() && wm.supportsAlerts()) {
                    WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.getWeatherAlerts());
                }

                // Update weather data for Wearables
                LocalBroadcastManager.getInstance(mContext)
                        .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));
            }
        }

        return Result.success();
    }

    private Weather getWeather() {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                Weather weather;

                try {
                    if (Settings.useFollowGPS())
                        updateLocation();

                    if (isCtsCancelRequested()) throw new InterruptedException();

                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());
                    weather = Tasks.await(wloader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .loadAlerts()
                            .loadForecasts()
                            .build()));

                    if (weather != null) {
                        // Re-schedule alarm at selected interval from now
                        enqueueWork(App.getInstance().getAppContext());
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
            public Boolean call() {
                boolean locationChanged = false;
                Context context = App.getInstance().getAppContext();

                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    LocationManager locMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 20, TimeUnit.SECONDS);
                                } catch (Exception e) {
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

                        if (isCtsCancelRequested()) return false;

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel query_vm;

                        TaskCompletionSource<LocationQueryViewModel> tcs = new TaskCompletionSource<>(cts.getToken());
                        try {
                            tcs.setResult(wm.getLocation(location));
                            query_vm = Tasks.await(tcs.getTask());
                        } catch (ExecutionException | WeatherException e) {
                            Logger.writeLine(Log.ERROR, e);
                            return false;
                        } catch (InterruptedException e) {
                            return false;
                        }

                        if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                            query_vm = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested()) return false;

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false));

                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }
}
