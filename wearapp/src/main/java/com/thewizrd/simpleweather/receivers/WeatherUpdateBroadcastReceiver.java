package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.wearable.WearableWorker;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_STARTALARM);
            // NOTE: above worker already executes every 2hrs; below worker executes every hr
            // WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_STARTALARM);

            final SettingsManager settingsMgr = new SettingsManager(context.getApplicationContext());
            if (settingsMgr.getDataSync() != WearableDataSync.OFF) {
                // Request a full update (force Settings refresh + weather + location)
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTSETTINGSUPDATE, true);
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTLOCATIONUPDATE, true);
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTWEATHERUPDATE); // too big to force request for update
            }
        }
    }
}
