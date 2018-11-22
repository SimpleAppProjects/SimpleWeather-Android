package com.thewizrd.simpleweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.wearable.WeatherComplicationIntentService;

public class CommonActionsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommonActionsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(intent.getAction()) ||
                    CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction()) ||
                    CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction())) {
                WeatherComplicationIntentService.enqueueWork(context,
                        new Intent(context, WeatherComplicationIntentService.class)
                                .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATIONS)
                                .putExtra(WeatherComplicationIntentService.EXTRA_FORCEUPDATE, true));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction())) {
                Settings.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
