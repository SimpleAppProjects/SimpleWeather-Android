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
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.wearable.WeatherComplicationIntentService;
import com.thewizrd.simpleweather.wearable.WeatherTileIntentService;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WeatherUpdaterWorker extends Worker {
    private static String TAG = "WeatherUpdaterWorker";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.Wear.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.Wear.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM";

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
                new PeriodicWorkRequest.Builder(WeatherUpdaterWorker.class, 120, TimeUnit.MINUTES, 30, TimeUnit.MINUTES)
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
        WorkManager.getInstance(context).cancelUniqueWork(TAG);
        Logger.writeLine(Log.INFO, "%s: Canceled work", TAG);
        return true;
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

            // Update complications
            WeatherComplicationIntentService.enqueueWork(mContext,
                    new Intent(mContext, WeatherComplicationIntentService.class)
                            .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATIONS));

            // Update tiles
            WeatherTileIntentService.enqueueWork(mContext,
                    new Intent(mContext, WeatherTileIntentService.class)
                            .setAction(WeatherTileIntentService.ACTION_UPDATETILES));
        }

        if (Settings.getDataSync() != WearableDataSync.OFF) {
            // Check if data has been updated
            WearableWorker.enqueueAction(mContext, WearableWorker.ACTION_REQUESTWEATHERUPDATE);
        }

        return Result.success();
    }

    private Weather getWeather() {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                Weather weather;

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
                            // Stop since there is no valid query
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
