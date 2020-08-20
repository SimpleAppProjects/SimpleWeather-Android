package com.thewizrd.simpleweather.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.R;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WeatherNotificationWorker extends Worker {
    private static final String TAG = "WeatherNotificationWorker";

    // Actions
    private static final String KEY_ACTION = "action";
    public static final String ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION";
    public static final String ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION";

    // Extras
    public static final String EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH";

    // Sets an ID for the notification
    private static final String NOT_CHANNEL_ID = "SimpleWeather.ongoingweather";

    private static final int JOB_ID = 1003;
    private static final int PERSISTENT_NOT_ID = JOB_ID;

    private final Context mContext;

    public WeatherNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();
    }

    public static void enqueueAction(@NonNull Context context, @NonNull Intent intent) {
        context = context.getApplicationContext();

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_REFRESHNOTIFICATION:
                case ACTION_REMOVENOTIFICATION:
                    startWork(context, intent);
                    break;
            }
        }
    }

    private static void startWork(@NonNull Context context, @NonNull Intent intent) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        Constraints.Builder constraints = new Constraints.Builder();
        if (ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
            constraints
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false);
        }

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WeatherNotificationWorker.class)
                .setConstraints(constraints.build())
                .setInputData(
                        new Data.Builder()
                                .putString(KEY_ACTION, intent.getAction())
                                .putBoolean(EXTRA_FORCEREFRESH, intent.getBooleanExtra(EXTRA_FORCEREFRESH, false))
                                .build()
                )
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(String.format(Locale.ROOT, "%s:%s_oneTime", TAG, intent.getAction()),
                        ExistingWorkPolicy.REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        final String intentAction = getInputData().getString(KEY_ACTION);
        final boolean forceRefresh = getInputData().getBoolean(EXTRA_FORCEREFRESH, false);

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction);

        if (ACTION_REFRESHNOTIFICATION.equals(intentAction)) {
            if (Settings.isWeatherLoaded()) {
                Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                    @Override
                    public Weather call() {
                        LocationData locData = Settings.getHomeData();
                        WeatherDataLoader wLoader = new WeatherDataLoader(locData);
                        WeatherRequest.Builder request = new WeatherRequest.Builder();
                        if (forceRefresh)
                            request.forceRefresh(false);
                        else
                            request.forceLoadSavedData();
                        try {
                            return Tasks.await(wLoader.loadWeatherData(request.build()));
                        } catch (ExecutionException | InterruptedException e) {
                            return null;
                        }
                    }
                });

                if (Settings.showOngoingNotification() && weather != null) {
                    // Gets an instance of the NotificationManager service
                    NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    initChannel(mNotifyMgr);

                    // Update notification
                    Notification mNotification = WeatherNotificationBuilder.updateNotification(NOT_CHANNEL_ID, new WeatherNowViewModel(weather));
                    mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification);
                } else if (!Settings.showOngoingNotification()) {
                    removeNotification();
                }
            }
        } else if (ACTION_REMOVENOTIFICATION.equals(intentAction)) {
            removeNotification();
        }

        return Result.success();
    }

    private void initChannel(NotificationManager mNotifyMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            if (mChannel == null) {
                String notchannel_name = mContext.getResources().getString(R.string.not_channel_name_weather);
                String notchannel_desc = mContext.getResources().getString(R.string.not_channel_desc_weather);

                mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription(notchannel_desc);
                // Configure the notification channel.
                mChannel.setShowBadge(true);
                mChannel.enableLights(false);
                mChannel.enableVibration(false);
                mNotifyMgr.createNotificationChannel(mChannel);
            }
        }
    }

    private void removeNotification() {
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(PERSISTENT_NOT_ID);
    }
}