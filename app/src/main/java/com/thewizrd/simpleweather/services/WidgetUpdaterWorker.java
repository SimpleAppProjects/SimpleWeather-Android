package com.thewizrd.simpleweather.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBroadcastReceiver;
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker;
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.widgets.WeatherWidgetBroadcastReceiver;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WidgetUpdaterWorker extends Worker {
    private static final String TAG = "WidgetUpdaterWorker";

    public static final String ACTION_UPDATEWIDGETS = "SimpleWeather.Droid.action.UPDATE_WIDGETS";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    private static final int JOB_ID = 1005;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.generalnotif";

    private final Context mContext;

    public WidgetUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        context = context.getApplicationContext();

        switch (intentAction) {
            case ACTION_UPDATEALARM:
                enqueueWork(context);
                break;
            case ACTION_STARTALARM:
            case ACTION_UPDATEWIDGETS:
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

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WidgetUpdaterWorker.class)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);

        // Enqueue periodic task as well
        enqueueWork(context);
    }

    private static void enqueueWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, Boolean.toString(isWorkScheduled(context)));

        PeriodicWorkRequest updateRequest =
                new PeriodicWorkRequest.Builder(WidgetUpdaterWorker.class, 60, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                        .setConstraints(Constraints.NONE)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, updateRequest);

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
                    || workStatus.getState() == WorkInfo.State.ENQUEUED;
        }
        return running;
    }

    private static boolean cancelWork(@NonNull Context context) {
        // Cancel alarm if dependent features are turned off
        context = context.getApplicationContext();
        if (!WeatherWidgetService.widgetsExist(context) && !Settings.showOngoingNotification()) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG);
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG);
            return true;
        }

        return false;
    }

    @NonNull
    @Override
    public Result doWork() {
        /*
        // Request work to be in foreground (only for Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(mContext), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)).get();
                } else {
                    setForegroundAsync(new ForegroundInfo(JOB_ID, getForegroundNotification(mContext))).get();
                }
            } catch (ExecutionException | InterruptedException e) {
                // no-op
            }
        }
        */

        if (Settings.isWeatherLoaded()) {
            if (WeatherWidgetService.widgetsExist(mContext)) {
                mContext.sendBroadcast(new Intent(mContext, WeatherWidgetBroadcastReceiver.class)
                        .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET));
            }

            if (Settings.showOngoingNotification()) {
                mContext.sendBroadcast(new Intent(mContext, WeatherNotificationBroadcastReceiver.class)
                        .setAction(WeatherNotificationWorker.ACTION_REFRESHNOTIFICATION));
            }

            ShortcutCreatorWorker.requestUpdateShortcuts(mContext);

            // Update weather data for Wearables
            WearableWorker.enqueueAction(mContext, WearableWorker.ACTION_SENDWEATHERUPDATE, false);
        }

        return Result.success();
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
                        .setSmallIcon(R.drawable.day_cloudy)
                        .setContentTitle(context.getString(R.string.not_title_weather_update))
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setOnlyAlertOnce(true)
                        .setNotificationSilent()
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        return mBuilder.build();
    }
}
