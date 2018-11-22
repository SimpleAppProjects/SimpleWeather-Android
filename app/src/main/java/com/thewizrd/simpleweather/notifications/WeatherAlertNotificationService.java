package com.thewizrd.simpleweather.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Simple service to keep track of posted alert notifications
@SuppressLint("UseSparseArrays")
public class WeatherAlertNotificationService extends JobIntentService {
    private static final String TAG = "WeatherAlertNotificationService";

    public static final String ACTION_CANCELNOTIFICATION = "SimpleWeather.Droid.action.CANCEL_NOTIFICATION";
    public static final String ACTION_CANCELALLNOTIFICATIONS = "SimpleWeather.Droid.action.CANCEL_ALL_NOTIFICATIONS";
    public static final String EXTRA_NOTIFICATIONID = "SimpleWeather.Droid.extra.NOTIFICATION_ID";

    private static final int JOB_ID = 1001;

    private static HashMap<Integer, String> mNotifications;

    static {
        if (mNotifications == null)
            mNotifications = new HashMap<>();
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherAlertNotificationService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mNotifications == null)
            mNotifications = new HashMap<>();
    }

    public static void addNotification(int notID, String title) {
        if (mNotifications == null)
            mNotifications = new HashMap<>();

        mNotifications.put(notID, title);
    }

    public static int getNotificationsCount() {
        if (mNotifications == null)
            return 0;
        else
            return mNotifications.size();
    }

    public static Set<Map.Entry<Integer, String>> getNotifications() {
        if (mNotifications != null)
            return mNotifications.entrySet();
        else
            return null;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_CANCELNOTIFICATION.equals(intent.getAction())) {
            int id = intent.getIntExtra(EXTRA_NOTIFICATIONID, -2);
            if (id >= 0 && mNotifications.size() > 0) {
                mNotifications.remove(id);
            } else if (ACTION_CANCELALLNOTIFICATIONS.equals(intent.getAction())) {
                mNotifications.clear();
            }
        }
    }
}
