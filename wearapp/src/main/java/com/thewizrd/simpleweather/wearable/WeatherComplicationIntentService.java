package com.thewizrd.simpleweather.wearable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.App;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;

public class WeatherComplicationIntentService extends JobIntentService {
    private static String TAG = "WeatherComplicationIntentService";

    public static final String ACTION_UPDATECOMPLICATIONS = "SimpleWeather.Droid.Wear.action.UPDATE_COMPLICATIONS";
    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.Wear.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM";

    public static final String EXTRA_FORCEUPDATE = "SimpleWeather.Droid.Wear.extra.FORCE_UPDATE";

    private Context mContext;
    private ProviderUpdateRequester updateRequester;
    private static boolean alarmStarted = false;

    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherComplicationIntentService.class,
                JOB_ID, work);
    }

    private boolean complicationsExist() {
        return WeatherComplicationService.complicationsExist();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        updateRequester = new ProviderUpdateRequester(mContext,
                new ComponentName(mContext, WeatherComplicationService.class));

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.writeLine(Log.ERROR, e, "%s: Unhandled Exception %s", TAG, e == null ? null : e.getMessage());

                if (oldHandler != null) {
                    oldHandler.uncaughtException(t, e);
                } else {
                    System.exit(2);
                }
            }
        });
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_UPDATECOMPLICATIONS.equals(intent.getAction())) {
            boolean force = intent.getBooleanExtra(EXTRA_FORCEUPDATE, false);

            if (Duration.between(LocalDateTime.now(), WeatherComplicationService.getUpdateTime()).toMinutes() > Settings.DEFAULTINTERVAL)
                force = true;

            if (force) {
                // Request updates
                updateRequester.requestUpdateAll();
                updateAlarm(App.getInstance().getAppContext());
            }
        } else if (ACTION_STARTALARM.equals(intent.getAction())) {
            startAlarm(App.getInstance().getAppContext());
        } else if (ACTION_CANCELALARM.equals(intent.getAction())) {
            cancelAlarms(App.getInstance().getAppContext());
        }
    }

    private PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, WeatherComplicationBroadcastReceiver.class)
                .setAction(ACTION_UPDATECOMPLICATIONS);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private void updateAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int interval = Settings.DEFAULTINTERVAL;

        boolean startNow = !alarmStarted;
        long intervalMillis = Duration.ofMinutes(interval).toMillis();
        long triggerAtTime = SystemClock.elapsedRealtime() + intervalMillis;

        if (startNow) {
            enqueueWork(context, new Intent(context, WeatherComplicationIntentService.class)
                    .setAction(ACTION_UPDATECOMPLICATIONS));
        }

        PendingIntent pendingIntent = getAlarmIntent(context);
        am.cancel(pendingIntent);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, intervalMillis, pendingIntent);
        alarmStarted = true;

        Logger.writeLine(Log.INFO, "%s: Updated alarm", TAG);
    }

    private void cancelAlarms(Context context) {
        // Cancel alarm if dependent features are turned off
        if (!complicationsExist()) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(getAlarmIntent(context));
            alarmStarted = false;

            Logger.writeLine(Log.INFO, "%s: Canceled alarm", TAG);
        }
    }

    private void startAlarm(Context context) {
        // Start alarm if dependent features are enabled
        if (!alarmStarted && complicationsExist()) {
            updateAlarm(context);
            alarmStarted = true;
        }
    }
}