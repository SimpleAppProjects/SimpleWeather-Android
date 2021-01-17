package com.thewizrd.simpleweather.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tzdb.TZDBCache;
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
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver;
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import timber.log.Timber;

public class WeatherUpdaterWorker extends ListenableWorker {
    private static final String TAG = "WeatherUpdaterWorker";

    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    private static final int JOB_ID = 1004;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.generalnotif";

    private final WeatherManager wm = WeatherManager.getInstance();

    private FusedLocationProviderClient mFusedLocationClient;
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
        final Context context = getApplicationContext();

        final boolean hasBackgroundLocationAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        /*
        // Request work to be in foreground (only for Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int foregroundServiceTypeFlags = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
                    if (hasBackgroundLocationAccess)
                        foregroundServiceTypeFlags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

                    setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(context), foregroundServiceTypeFlags)).get();
                } else {
                    setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(context))).get();
                }
            } catch (ExecutionException | InterruptedException e) {
                // no-op
            }
        }
        */

        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()).submit(new Callable<Result>() {
            @Override
            public Result call() {
                Logger.writeLine(Log.INFO, "%s: Work started", TAG);

                // Update configuration
                RemoteConfig.checkConfig();

                if (Settings.isWeatherLoaded()) {
                    if (Settings.useFollowGPS()) {
                        try {
                            updateLocation().get();
                        } catch (ExecutionException | InterruptedException e) {
                            Logger.writeLine(Log.ERROR, e);
                            if (hasBackgroundLocationAccess) {
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

                    if (WeatherWidgetService.widgetsExist(context)) {
                        context.sendBroadcast(new Intent(context, WeatherWidgetBroadcastReceiver.class)
                                .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET));
                    }

                    if (weather != null) {
                        if (Settings.showOngoingNotification()) {
                            context.sendBroadcast(new Intent(context, WeatherNotificationBroadcastReceiver.class)
                                    .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION));
                        }

                        if (Settings.useAlerts() && wm.supportsAlerts()) {
                            WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.getWeatherAlerts());
                        }

                        // Update weather data for Wearables
                        LocalBroadcastManager.getInstance(context)
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
        final Context context = getApplicationContext();

        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()).submit(new Callable<Boolean>() {
            @SuppressLint("MissingPermission")
            @Override
            public Boolean call() {
                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    final LocationManager locMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        return false;
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
                            mLocationRequest.setExpirationDuration(60000);

                            Looper.prepare();

                            Timber.tag(TAG).i("Fused: Requesting location updates...");
                            final TaskCompletionSource<LocationResult> tcs = new TaskCompletionSource<>(cts.getToken());
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    mFusedLocationClient.removeLocationUpdates(this);

                                    Timber.tag(TAG).i("Fused: Location update received...");
                                    tcs.trySetResult(locationResult);
                                }

                                @Override
                                public void onLocationAvailability(LocationAvailability locationAvailability) {
                                    super.onLocationAvailability(locationAvailability);

                                    if (!locationAvailability.isLocationAvailable()) {
                                        mFusedLocationClient.removeLocationUpdates(this);
                                        tcs.trySetResult(null);
                                    }
                                }
                            }, Looper.myLooper());

                            try {
                                LocationResult locationResult = Tasks.await(tcs.getTask());

                                if (locationResult != null) {
                                    location = locationResult.getLastLocation();
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                // no-op
                            }
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

                                Timber.tag(TAG).i("LocMan: Requesting location update...");
                                final TaskCompletionSource<Location> tcs = new TaskCompletionSource<>(cts.getToken());
                                LocationListener locationListener = new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        locMan.removeUpdates(this);

                                        Timber.tag(TAG).i("LocMan: Location update received...");
                                        tcs.setResult(location);
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
                                };
                                locMan.requestSingleUpdate(provider, locationListener, Looper.myLooper());

                                try {
                                    location = Tasks.await(tcs.getTask(), 60, TimeUnit.SECONDS);
                                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                                    locMan.removeUpdates(locationListener);
                                }
                            }
                        }
                    }

                    if (location != null && !isCtsCancelRequested()) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        if (isCtsCancelRequested()) {
                            return false;
                        }

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
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
                            return false;
                        } else if (StringUtils.isNullOrWhitespace(query_vm.getLocationTZLong()) && query_vm.getLocationLat() != 0 && query_vm.getLocationLong() != 0) {
                            String tzId = TZDBCache.getTimeZone(query_vm.getLocationLat(), query_vm.getLocationLong());
                            if (!"unknown".equals(tzId))
                                query_vm.setLocationTZLong(tzId);
                        }

                        if (isCtsCancelRequested()) {
                            return false;
                        }

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
                                        .putExtra(CommonActions.EXTRA_FORCEUPDATE, false));

                        return true;
                    }
                }

                return false;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
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

    @TargetApi(Build.VERSION_CODES.O)
    private static Notification getForegroundNotification(@NonNull final Context context) {
        initChannel(context);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.wi_day_cloudy)
                        .setContentTitle(context.getString(R.string.not_title_weather_update))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setOnlyAlertOnce(true)
                        .setNotificationSilent()
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        return mBuilder.build();
    }
}
