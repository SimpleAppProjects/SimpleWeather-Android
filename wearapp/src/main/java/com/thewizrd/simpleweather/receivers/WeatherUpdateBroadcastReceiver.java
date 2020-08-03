package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_STARTALARM);

            if (Settings.getDataSync() != WearableDataSync.OFF) {
                // Request a full update
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_REQUESTSETTINGSUPDATE));
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_REQUESTLOCATIONUPDATE));
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE));
            }
        }
    }
}
