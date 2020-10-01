package com.thewizrd.simpleweather.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.LaunchActivity;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.updates.InAppUpdateManager;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AppUpdaterWorker extends Worker {
    private static String TAG = "AppUpdaterWorker";

    // Sets an ID for the notification
    private static final String NOT_CHANNEL_ID = "SimpleWeather.appupdates";

    public AppUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void registerWorker(@NonNull Context context) {
        context = context.getApplicationContext();
        enqueueWork(context);
    }

    private static void enqueueWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, Boolean.toString(isWorkScheduled(context)));

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(false)
                .build();

        PeriodicWorkRequest updateRequest =
                new PeriodicWorkRequest.Builder(AppUpdaterWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
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
                    | workStatus.getState() == WorkInfo.State.ENQUEUED;
        }
        return running;
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        // Creates instance of the manager.
        InAppUpdateManager appUpdateManager = InAppUpdateManager.create(getApplicationContext());

        if (appUpdateManager.checkIfUpdateAvailable()) {
            if (appUpdateManager.getUpdatePriority() > 3 && !FeatureSettings.isUpdateAvailable()) {
                // Notify user of update availability
                NotificationManager mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                initChannel(mNotifyMgr);

                NotificationCompat.Builder mNotif = new NotificationCompat.Builder(getApplicationContext(), NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_error_white)
                        .setContentTitle(getApplicationContext().getString(R.string.prompt_update_title))
                        .setContentText(getApplicationContext().getString(R.string.prompt_update_available))
                        .setContentIntent(getLaunchUpdatesIntent(getApplicationContext()))
                        .setColor(Colors.SIMPLEBLUE)
                        .setAutoCancel(true);

                mNotifyMgr.notify((int) (SystemClock.uptimeMillis() + appUpdateManager.getUpdatePriority()), mNotif.build());
            }
        }

        return Result.success();
    }

    private static PendingIntent getLaunchUpdatesIntent(@NonNull Context context) {
        Intent i = new Intent(context.getApplicationContext(), LaunchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return PendingIntent.getActivity(context.getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void initChannel(NotificationManager mNotifyMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            // "App Updates"
            String notchannel_name = getApplicationContext().getResources().getString(R.string.not_channel_update_title);

            if (mChannel == null) {
                mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW);
            }
            mChannel.setName(notchannel_name);
            // Configure the notification channel.
            mChannel.setShowBadge(true);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            mNotifyMgr.createNotificationChannel(mChannel);
        }
    }
}
