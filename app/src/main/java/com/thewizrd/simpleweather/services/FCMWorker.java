package com.thewizrd.simpleweather.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper;
import com.thewizrd.shared_resources.weatherdata.images.ImageDatabase;

import java.util.concurrent.TimeUnit;

public class FCMWorker extends Worker {
    private static String TAG = "FCMWorker";

    public static final String ACTION_INVALIDATE = "SimpleWeather.Droid.action.INVALIDATE";

    public FCMWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        context = context.getApplicationContext();

        if (ACTION_INVALIDATE.equals(intentAction)) {
            startWork(context);
        }
    }

    private static void startWork(@NonNull Context context) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(FCMWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG + "_onBoot", ExistingWorkPolicy.REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        // Check if cache is populated
        if (!ImageDataHelper.getImageDataHelper().isEmpty()) {
            // If so, check if we need to invalidate
            // if so, invalidate
            long updateTime = ImageDatabase.getLastUpdateTime();

            if (updateTime != ImageDataHelper.getImageDBUpdateTime()) {
                ImageDataHelper.setImageDBUpdateTime(updateTime);

                ImageDataHelper.getImageDataHelper().clearCachedImageData();
                ImageDataHelper.invalidateCache(true);
            }
        }

        return Result.success();
    }
}
