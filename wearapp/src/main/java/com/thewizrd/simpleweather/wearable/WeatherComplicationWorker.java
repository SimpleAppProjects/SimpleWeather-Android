package com.thewizrd.simpleweather.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;
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

import com.thewizrd.shared_resources.utils.Logger;

import java.util.Locale;

public class WeatherComplicationWorker extends Worker {
    private static final String TAG = "WeatherComplicationWorker";

    // Actions
    private static final String KEY_ACTION = "action";
    public static final String ACTION_UPDATECOMPLICATION = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATION";
    public static final String ACTION_UPDATECOMPLICATIONS = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATIONS";

    // Extras
    public static final String EXTRA_COMPLICATIONID = "SimpleWeather.Droid.Wear.extra.COMPLICATION_ID";

    private ProviderUpdateRequester updateRequester;

    public WeatherComplicationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Context mContext = getApplicationContext();
        updateRequester = new ProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherComplicationService.class));
    }

    public static void enqueueAction(@NonNull Context context, @NonNull Intent intent) {
        context = context.getApplicationContext();

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_UPDATECOMPLICATION:
                case ACTION_UPDATECOMPLICATIONS:
                    startWork(context, intent);
                    break;
            }
        }
    }

    private static void startWork(@NonNull Context context, @NonNull Intent intent) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WeatherComplicationWorker.class)
                .setConstraints(constraints)
                .setInputData(
                        new Data.Builder()
                                .putString(KEY_ACTION, intent.getAction())
                                .putInt(EXTRA_COMPLICATIONID, intent.getIntExtra(EXTRA_COMPLICATIONID, 0))
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
        final int complicationId = getInputData().getInt(EXTRA_COMPLICATIONID, 0);

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction);

        if (ACTION_UPDATECOMPLICATIONS.equals(intentAction)) {
            // Request updates
            updateRequester.requestUpdateAll();
        } else if (ACTION_UPDATECOMPLICATION.equals(intentAction)) {
            updateRequester.requestUpdate(complicationId);
        }

        return Result.success();
    }
}