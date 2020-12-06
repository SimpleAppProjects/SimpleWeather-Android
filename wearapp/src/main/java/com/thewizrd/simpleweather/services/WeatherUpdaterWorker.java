package com.thewizrd.simpleweather.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
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
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.wearable.WeatherComplicationWorker;
import com.thewizrd.simpleweather.wearable.WeatherTileWorker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WeatherUpdaterWorker extends ListenableWorker {
    private static final String TAG = "WeatherUpdaterWorker";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.Wear.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.Wear.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM";

    private static final int JOB_ID = 1000;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.generalnotif";

    private FusedLocationProviderClient mFusedLocationClient;

    private final WeatherManager wm = WeatherManager.getInstance();
    private final CancellationTokenSource cts = new CancellationTokenSource();

    public WeatherUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        }
    }

    private boolean isCtsCancelRequested() {
        return cts.getToken().isCancellationRequested();
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

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WeatherUpdaterWorker.class)
                .setInitialDelay(60, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);

        // Enqueue periodic task as well
        enqueueWork(context);
    }

    private static void enqueueWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting work", TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        PeriodicWorkRequest updateRequest =
                new PeriodicWorkRequest.Builder(WeatherUpdaterWorker.class, Settings.DEFAULTINTERVAL, TimeUnit.MINUTES, 30, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, updateRequest);

        Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG);
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
        cts.cancel();
        super.onStopped();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        final Context context = getApplicationContext();

        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()).submit(new Callable<Result>() {
            @Override
            public Result call() {
                Logger.writeLine(Log.INFO, "%s: Work started", TAG);

                // Request work to be in foreground (only for Oreo+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(context), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)).get();
                        } else {
                            setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(context))).get();
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        // no-op
                    }
                }

                if (Settings.isWeatherLoaded()) {
                    if (Settings.getDataSync() == WearableDataSync.OFF && Settings.useFollowGPS()) {
                        try {
                            updateLocation().get();
                        } catch (ExecutionException | InterruptedException e) {
                            Logger.writeLine(Log.ERROR, e);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                return Result.retry();
                            }
                        }
                    }

                    // Update for home
                    final Weather weather = AsyncTask.await(new Callable<Weather>() {
                        @Override
                        public Weather call() {
                            return getWeather();
                        }
                    });

                    if (weather != null) {
                        // Update complications
                        WeatherComplicationWorker.enqueueAction(context, new Intent(WeatherComplicationWorker.ACTION_UPDATECOMPLICATIONS));

                        // Update tiles
                        WeatherTileWorker.enqueueAction(context, new Intent(WeatherTileWorker.ACTION_UPDATETILES));
                    } else {
                        if (Settings.getDataSync() != WearableDataSync.OFF) {
                            // Check if data has been updated
                            WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTWEATHERUPDATE);
                        }
                        return Result.retry();
                    }
                }

                return Result.success();
            }
        });
    }

    private Weather getWeather() {
        return AsyncTask.await(new Callable<Weather>() {
            @Override
            public Weather call() {
                Weather weather;

                try {
                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());

                    WeatherRequest.Builder request = new WeatherRequest.Builder();
                    if (Settings.getDataSync() == WearableDataSync.OFF) {
                        request.forceRefresh(false).loadAlerts();
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

    private ListenableFuture<Boolean> updateLocation() {
        final SettableFuture<Boolean> result = SettableFuture.create();
        final Context context = getApplicationContext();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        result.set(false);
                        return;
                    }

                    Location location = null;

                    final LocationManager locMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        result.set(false);
                        return;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = AsyncTask.await(new Callable<Location>() {
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

                        if (location == null) {
                            final LocationRequest mLocationRequest = new LocationRequest();
                            mLocationRequest.setNumUpdates(1);
                            mLocationRequest.setInterval(10000);
                            mLocationRequest.setFastestInterval(1000);
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                            Looper.prepare();

                            Log.i(TAG, "Fused: Requesting location updates...");
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    mFusedLocationClient.removeLocationUpdates(this);

                                    Log.i(TAG, "Fused: Location update received...");
                                    result.setFuture(updateLocation());
                                }

                                @Override
                                public void onLocationAvailability(LocationAvailability locationAvailability) {
                                    super.onLocationAvailability(locationAvailability);
                                }
                            }, Looper.myLooper());

                            return;
                        }
                    } else {
                        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                        if ((isGPSEnabled || isNetEnabled) && !isCtsCancelRequested()) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);

                            if (location == null) {
                                Looper.prepare();

                                Log.i(TAG, "LocMan: Requesting location update...");
                                locMan.requestSingleUpdate(provider, new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        locMan.removeUpdates(this);

                                        Log.i(TAG, "LocMan: Location update received...");
                                        result.setFuture(updateLocation());
                                    }

                                    @Override
                                    public void onStatusChanged(String s, int i, Bundle bundle) {

                                    }

                                    @Override
                                    public void onProviderEnabled(String s) {

                                    }

                                    @Override
                                    public void onProviderDisabled(String s) {

                                    }
                                }, Looper.myLooper());

                                return;
                            }
                        }
                    }

                    if (location != null && !isCtsCancelRequested()) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        if (isCtsCancelRequested()) {
                            result.set(false);
                            return;
                        }

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            result.set(false);
                            return;
                        }

                        LocationQueryViewModel query_vm;

                        try {
                            final Location finalLocation = location;
                            query_vm = AsyncTask.await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                                @Override
                                public LocationQueryViewModel call() throws WeatherException {
                                    return wm.getLocation(finalLocation);
                                }
                            }, cts.getToken());
                        } catch (WeatherException e) {
                            Logger.writeLine(Log.ERROR, e);
                            result.set(false);
                            return;
                        }

                        if (query_vm == null || StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            result.set(false);
                            return;
                        }

                        if (isCtsCancelRequested()) {
                            result.set(false);
                            return;
                        }

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        result.set(true);
                        return;
                    }
                }

                result.set(false);
            }
        });

        return result;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void initChannel(@NonNull final Context context) {
        // Gets an instance of the NotificationManager service
        final NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);
        final String notchannel_name = context.getResources().getString(R.string.not_channel_name_general);

        if (mChannel == null) {
            mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW);
        }

        // Configure the notification channel.
        mChannel.setName(notchannel_name);
        mChannel.setShowBadge(false);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mNotifyMgr.createNotificationChannel(mChannel);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static Notification getForegroundNotification(@NonNull final Context context) {
        initChannel(context);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.day_cloudy)
                        .setContentTitle(context.getString(R.string.not_title_weather_update))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setOnlyAlertOnce(true)
                        .setNotificationSilent()
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        return mBuilder.build();
    }
}
