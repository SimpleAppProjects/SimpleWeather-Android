package com.thewizrd.simpleweather.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
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
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver;
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WeatherUpdaterWorker extends ListenableWorker {
    private static final String TAG = "WeatherUpdaterWorker";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    private final Context mContext;

    private final WeatherManager wm = WeatherManager.getInstance();

    private FusedLocationProviderClient mFusedLocationClient;
    private final CancellationTokenSource cts = new CancellationTokenSource();

    public WeatherUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(mContext);
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
                new PeriodicWorkRequest.Builder(WeatherUpdaterWorker.class, Settings.getRefreshInterval(), TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, updateRequest);

        Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG);
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
        cts.cancel();
        super.onStopped();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()).submit(new Callable<Result>() {
            @Override
            public Result call() {
                Logger.writeLine(Log.INFO, "%s: Work started", TAG);

                if (Settings.isWeatherLoaded()) {
                    if (Settings.useFollowGPS()) {
                        try {
                            updateLocation().get();
                        } catch (ExecutionException | InterruptedException e) {
                            Logger.writeLine(Log.ERROR, e);
                            return Result.retry();
                        }
                    }

                    // Update for home
                    final Weather weather = AsyncTask.await(new Callable<Weather>() {
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
                                    .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION));
                        }

                        if (Settings.useAlerts() && wm.supportsAlerts()) {
                            WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.getWeatherAlerts());
                        }

                        // Update weather data for Wearables
                        LocalBroadcastManager.getInstance(mContext)
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE));
                    } else {
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
                    weather = Tasks.await(wloader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .loadAlerts()
                            .loadForecasts()
                            .build()));

                    if (weather != null) {
                        // Re-schedule alarm at selected interval from now
                        enqueueWork(App.getInstance().getAppContext());
                        Settings.setUpdateTime(LocalDateTime.now(ZoneOffset.UTC));
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

    private ListenableFuture<Boolean> updateLocation() {
        final SettableFuture<Boolean> result = SettableFuture.create();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        result.set(false);
                        return;
                    }

                    Location location = null;

                    final LocationManager locMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

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

                        final Location finalLocation = location;
                        query_vm = AsyncTask.await(new Callable<LocationQueryViewModel>() {
                            @Override
                            public LocationQueryViewModel call() throws Exception {
                                return wm.getLocation(finalLocation);
                            }
                        }, cts.getToken());

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

                        LocalBroadcastManager.getInstance(mContext)
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false));

                        result.set(true);
                        return;
                    }
                }

                result.set(false);
            }
        });

        return result;
    }
}
