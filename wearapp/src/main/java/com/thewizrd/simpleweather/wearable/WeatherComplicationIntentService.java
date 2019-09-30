package com.thewizrd.simpleweather.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

public class WeatherComplicationIntentService extends JobIntentService {
    private static String TAG = "WeatherComplicationIntentService";

    public static final String ACTION_UPDATECOMPLICATION = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATION";
    public static final String ACTION_UPDATECOMPLICATIONS = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATIONS";

    public static final String EXTRA_FORCEUPDATE = "SimpleWeather.Droid.Wear.extra.FORCE_UPDATE";
    public static final String EXTRA_COMPLICATIONID = "SimpleWeather.Droid.Wear.extra.COMPLICATION_ID";

    private Context mContext;
    private ProviderUpdateRequester updateRequester;

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherComplicationIntentService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        updateRequester = new ProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherComplicationService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATECOMPLICATIONS.equals(intent.getAction())) {
            boolean force = intent.getBooleanExtra(EXTRA_FORCEUPDATE, false);

            if (Duration.between(LocalDateTime.now(), WeatherComplicationService.getUpdateTime()).toMinutes() > Settings.getRefreshInterval())
                force = true;

            if (force) {
                // Request updates
                updateRequester.requestUpdateAll();
            }
        } else if (ACTION_UPDATECOMPLICATION.equals(intent.getAction())) {
            updateRequester.requestUpdate(intent.getIntExtra(EXTRA_COMPLICATIONID, 0));
        } else {
            Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
        }
    }
}