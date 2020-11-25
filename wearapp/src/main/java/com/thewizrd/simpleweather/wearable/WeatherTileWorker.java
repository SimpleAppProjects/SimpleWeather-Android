package com.thewizrd.simpleweather.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import com.google.android.clockwork.tiles.TileProviderUpdateRequester;
import com.thewizrd.shared_resources.utils.Logger;

import java.util.Locale;

public class WeatherTileWorker extends Worker {
    private static final String TAG = "WeatherTileWorker";

    // Actions
    private static final String KEY_ACTION = "action";
    public static final String ACTION_UPDATETILE = "SimpleWeather.Droid.Wear.action.UPDATE_TILE";
    public static final String ACTION_UPDATETILES = "SimpleWeather.Droid.Wear.action.UPDATE_TILES";

    // Extras
    public static final String EXTRA_TILEID = "SimpleWeather.Droid.Wear.extra.TILE_ID";

    private final TileProviderUpdateRequester updateRequester;

    public WeatherTileWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Context mContext = getApplicationContext();
        updateRequester = new TileProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherTileProviderService.class));
    }

    public static void enqueueAction(@NonNull Context context, @NonNull Intent intent) {
        context = context.getApplicationContext();

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_UPDATETILE:
                case ACTION_UPDATETILES:
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

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WeatherTileWorker.class)
                .setConstraints(constraints)
                .setInputData(
                        new Data.Builder()
                                .putString(KEY_ACTION, intent.getAction())
                                .putInt(EXTRA_TILEID, intent.getIntExtra(EXTRA_TILEID, 0))
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
        final int tileId = getInputData().getInt(EXTRA_TILEID, 0);

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction);

        if (ACTION_UPDATETILES.equals(intentAction)) {
            // Request updates
            updateRequester.requestUpdateAll();
        } else if (ACTION_UPDATETILE.equals(intentAction)) {
            updateRequester.requestUpdate(tileId);
        }

        return Result.success();
    }
}