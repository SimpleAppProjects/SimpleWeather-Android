package com.thewizrd.simpleweather.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
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

import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper;
import com.thewizrd.shared_resources.weatherdata.images.ImageDatabase;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ImageDatabaseWorker extends Worker {
    private static String TAG = "ImageDatabaseWorker";

    public static final String ACTION_CHECKUPDATETIME = "SimpleWeather.Droid.action.CHECK_UPDATE_TIME";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    public ImageDatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        context = context.getApplicationContext();

        if (ACTION_UPDATEALARM.equals(intentAction)) {
            enqueueWork(context);
        } else if (ACTION_CHECKUPDATETIME.equals(intentAction) || ACTION_STARTALARM.equals(intentAction)) {
            // For immediate action
            startWork(context);
        } else if (ACTION_CANCELALARM.equals(intentAction)) {
            cancelWork(context);
        }
    }

    private static void startWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(ImageDatabaseWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);

        // Enqueue periodic task as well
        enqueueWork(context);
    }

    private static void enqueueWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, Boolean.toString(isWorkScheduled(context)));

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        PeriodicWorkRequest updateRequest =
                new PeriodicWorkRequest.Builder(ImageDatabaseWorker.class, 7, TimeUnit.DAYS)
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


    private static void cancelWork(@NonNull Context context) {
        context = context.getApplicationContext();
        WorkManager.getInstance(context).cancelUniqueWork(TAG);
        Logger.writeLine(Log.INFO, "%s: Canceled work", TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        // Check if cache is populated
        if (!ImageDataHelper.getImageDataHelper().isEmpty() && !FeatureSettings.isUpdateAvailable()) {
            // If so, check if we need to invalidate
            long updateTime = 0L;
            try {
                updateTime = Tasks.await(ImageDatabase.getLastUpdateTime());
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }

            if (updateTime > ImageDataHelper.getImageDBUpdateTime()) {
                AnalyticsLogger.logEvent(TAG + ": clearing image cache");

                // if so, invalidate
                ImageDataHelper.setImageDBUpdateTime(updateTime);

                ImageDataHelper.getImageDataHelper().clearCachedImageData();
                ImageDataHelper.invalidateCache(true);
            }
        }

        return Result.success();
    }
}
