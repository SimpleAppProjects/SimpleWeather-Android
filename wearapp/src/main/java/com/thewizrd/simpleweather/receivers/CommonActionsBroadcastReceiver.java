package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker;

public class CommonActionsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommonActionsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(intent.getAction()) ||
                    CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction())) {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction()) ||
                    CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                WidgetUpdaterWorker.requestWidgetUpdate(context);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEDATASYNC.equals(intent.getAction())) {
                // Reset UpdateTime value to force a refresh
                Settings.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
