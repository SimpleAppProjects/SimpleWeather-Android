package com.thewizrd.simpleweather.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.android.clockwork.tiles.TileProviderUpdateRequester;
import com.thewizrd.shared_resources.utils.Logger;

public class WeatherTileIntentService extends JobIntentService {
    private static String TAG = "WeatherTileIntentService";

    public static final String ACTION_UPDATETILE = "SimpleWeather.Droid.Wear.action.UPDATE_TILE";
    public static final String ACTION_UPDATETILES = "SimpleWeather.Droid.Wear.action.UPDATE_TILES";

    public static final String EXTRA_TILEID = "SimpleWeather.Droid.Wear.extra.TILE_ID";

    private TileProviderUpdateRequester updateRequester;

    private static final int JOB_ID = 1002;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherTileIntentService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Context mContext = getApplicationContext();
        updateRequester = new TileProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherTileProviderService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATETILES.equals(intent.getAction())) {
            // Request updates
            updateRequester.requestUpdateAll();
        } else if (ACTION_UPDATETILE.equals(intent.getAction())) {
            updateRequester.requestUpdate(intent.getIntExtra(EXTRA_TILEID, 0));
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action: %s", TAG, intent.getAction());
    }
}