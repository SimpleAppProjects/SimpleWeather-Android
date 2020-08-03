package com.thewizrd.simpleweather.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.thewizrd.shared_resources.utils.Logger;

public class WeatherComplicationIntentService extends JobIntentService {
    private static String TAG = "WeatherComplicationIntentService";

    public static final String ACTION_UPDATECOMPLICATION = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATION";
    public static final String ACTION_UPDATECOMPLICATIONS = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATIONS";

    public static final String EXTRA_COMPLICATIONID = "SimpleWeather.Droid.Wear.extra.COMPLICATION_ID";

    private ProviderUpdateRequester updateRequester;

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherComplicationIntentService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Context mContext = getApplicationContext();
        updateRequester = new ProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherComplicationService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATECOMPLICATIONS.equals(intent.getAction())) {
            // Request updates
            updateRequester.requestUpdateAll();
        } else if (ACTION_UPDATECOMPLICATION.equals(intent.getAction())) {
            updateRequester.requestUpdate(intent.getIntExtra(EXTRA_COMPLICATIONID, 0));
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action: %s", TAG, intent.getAction());
    }
}