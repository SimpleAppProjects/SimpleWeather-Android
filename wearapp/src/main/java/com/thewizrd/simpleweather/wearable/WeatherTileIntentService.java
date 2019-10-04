package com.thewizrd.simpleweather.wearable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.android.clockwork.tiles.TileProviderUpdateRequester;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

public class WeatherTileIntentService extends JobIntentService {
    private static String TAG = "WeatherTileIntentService";

    public static final String ACTION_UPDATETILE = "SimpleWeather.Droid.Wear.action.UPDATE_TILE";
    public static final String ACTION_UPDATETILES = "SimpleWeather.Droid.Wear.action.UPDATE_TILES";
    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.Wear.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM";

    public static final String EXTRA_FORCEUPDATE = "SimpleWeather.Droid.Wear.extra.FORCE_UPDATE";
    public static final String EXTRA_TILEID = "SimpleWeather.Droid.Wear.extra.TILE_ID";

    private Context mContext;
    private TileProviderUpdateRequester updateRequester;
    private static boolean mAlarmStarted = false;

    private static final int JOB_ID = 1002;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherTileIntentService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        updateRequester = new TileProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherTileProviderService.class));

        // Check if alarm is already set
        mAlarmStarted = (PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, WearableBroadcastReceiver.class)
                        .setAction(ACTION_UPDATETILES)
                        .putExtra(EXTRA_FORCEUPDATE, true),
                PendingIntent.FLAG_NO_CREATE) != null);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATETILES.equals(intent.getAction())) {
            boolean force = intent.getBooleanExtra(EXTRA_FORCEUPDATE, false);

            if (Duration.between(LocalDateTime.now(ZoneOffset.UTC), WeatherTileProviderService.getUpdateTime()).toMinutes() > Settings.getRefreshInterval())
                force = true;

            if (force) {
                // Request updates
                updateRequester.requestUpdateAll();
            }
        } else if (ACTION_UPDATETILE.equals(intent.getAction())) {
            updateRequester.requestUpdate(intent.getIntExtra(EXTRA_TILEID, 0));
        } else if (ACTION_STARTALARM.equals(intent.getAction())) {
            startAlarm(mContext);
        } else if (ACTION_UPDATEALARM.equals(intent.getAction())) {
            updateAlarm(mContext);
        } else if (ACTION_CANCELALARM.equals(intent.getAction())) {
            cancelAlarms(mContext);
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action: %s", TAG, intent.getAction());
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, WearableBroadcastReceiver.class)
                .setAction(ACTION_UPDATETILES)
                .putExtra(EXTRA_FORCEUPDATE, true);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static void updateAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final int interval = 120;// 120min - 2 hrs

        boolean startNow = !mAlarmStarted;
        long intervalMillis = Duration.ofMinutes(interval).toMillis();
        long triggerAtTime = SystemClock.elapsedRealtime() + intervalMillis;

        if (startNow) {
            enqueueWork(context, new Intent(context, WeatherTileProviderService.class)
                    .setAction(ACTION_UPDATETILES)
                    .putExtra(EXTRA_FORCEUPDATE, true));
        }

        PendingIntent pendingIntent = getAlarmIntent(context);
        am.cancel(pendingIntent);
        am.set(AlarmManager.ELAPSED_REALTIME, triggerAtTime, pendingIntent);
        mAlarmStarted = true;

        Logger.writeLine(Log.INFO, "%s: Updated alarm", TAG);
    }

    private static void cancelAlarms(Context context) {
        // Cancel alarm
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getAlarmIntent(context));
        mAlarmStarted = false;

        Logger.writeLine(Log.INFO, "%s: Canceled alarm", TAG);
    }

    private void startAlarm(Context context) {
        // Start alarm if dependent features are enabled
        if (!mAlarmStarted) {
            updateAlarm(context);
            mAlarmStarted = true;
        }
    }
}