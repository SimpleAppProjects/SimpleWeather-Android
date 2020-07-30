package com.thewizrd.simpleweather.notifications;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.main.MainActivity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Simple service to keep track of posted alert notifications
@SuppressLint("UseSparseArrays")
public class WeatherAlertNotificationService extends JobIntentService {
    private static final String TAG = "WeatherAlertNotificationService";

    public static final String ACTION_SHOWALERTS = "SimpleWeather.Droid.action.SHOW_ALERTS";

    public static final String ACTION_CANCELNOTIFICATION = "SimpleWeather.Droid.action.CANCEL_NOTIFICATION";
    public static final String ACTION_CANCELALLNOTIFICATIONS = "SimpleWeather.Droid.action.CANCEL_ALL_NOTIFICATIONS";
    public static final String EXTRA_NOTIFICATIONID = "SimpleWeather.Droid.extra.NOTIFICATION_ID";

    private static final int JOB_ID = 1001;

    private static HashMap<Integer, String> mNotifications;

    // Shared Settings
    private static SharedPreferences notifPrefs = App.getInstance().getAppContext().getSharedPreferences("notifications", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = notifPrefs.edit();

    private static final String KEY_NOTIFS = "notifications";

    static {
        initialize();
    }

    private static void initialize() {
        if (mNotifications == null) {
            String listJson = notifPrefs.getString(KEY_NOTIFS, "");
            if (!StringUtils.isNullOrWhitespace(listJson)) {
                Type mapKeyValue = new TypeToken<HashMap<Integer, String>>() {
                }.getType();
                HashMap<Integer, String> map = JSONParser.deserializer(listJson, mapKeyValue);
                if (map != null) {
                    mNotifications = map;
                }
            } else {
                mNotifications = new HashMap<>();
            }
        }
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherAlertNotificationService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mNotifications == null) initialize();
    }

    @Override
    public void onDestroy() {
        String json = JSONParser.serializer(mNotifications, HashMap.class);
        editor.putString(KEY_NOTIFS, json).commit();

        super.onDestroy();
    }

    public static void addNotification(int notID, String title) {
        if (mNotifications == null) initialize();
        mNotifications.put(notID, title);
    }

    public static int getNotificationsCount() {
        if (mNotifications == null)
            return 0;
        else
            return mNotifications.size();
    }

    public static Set<Map.Entry<Integer, String>> getNotifications() {
        return mNotifications.entrySet();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_CANCELNOTIFICATION.equals(intent.getAction())) {
            int id = intent.getIntExtra(EXTRA_NOTIFICATIONID, -2);
            if (id >= 0 && mNotifications.size() > 0)
                mNotifications.remove(id);
        } else if (ACTION_CANCELALLNOTIFICATIONS.equals(intent.getAction())) {
            mNotifications.clear();

            /*
             * NOTE
             * Compat issue: Part of workaround for setAutoCancel not working for JellyBean
             */
            if (intent.getBooleanExtra(WeatherAlertNotificationService.ACTION_SHOWALERTS, false)) {
                Intent appIntent = new Intent(this, MainActivity.class)
                        .setAction(WeatherAlertNotificationService.ACTION_SHOWALERTS)
                        .putExtra(WeatherAlertNotificationService.ACTION_SHOWALERTS, true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                this.startActivity(appIntent);
            }
        }
    }
}
