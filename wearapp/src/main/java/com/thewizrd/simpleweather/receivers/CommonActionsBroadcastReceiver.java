package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.simpleweather.wearable.WeatherComplicationIntentService;
import com.thewizrd.simpleweather.wearable.WeatherTileProviderService;

public class CommonActionsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommonActionsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(intent.getAction()) ||
                    CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction()) ||
                    CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction()) ||
                    CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                // Update complications
                WeatherComplicationIntentService.enqueueWork(context,
                        new Intent(context, WeatherComplicationIntentService.class)
                                .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATIONS)
                                .putExtra(WeatherComplicationIntentService.EXTRA_FORCEUPDATE, true));

                // Update tile
                context.startService(new Intent(context, WeatherTileProviderService.class)
                        .setAction(WeatherTileProviderService.ACTION_UPDATETILE)
                        .putExtra(WeatherTileProviderService.EXTRA_FORCEUPDATE, true));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEDATASYNC.equals(intent.getAction())) {
                // Reset interval if setting is off
                if (Settings.getDataSync() == WearableDataSync.OFF)
                    Settings.setRefreshInterval(Settings.DEFAULTINTERVAL);
                // Reset UpdateTime value to force a refresh
                Settings.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
