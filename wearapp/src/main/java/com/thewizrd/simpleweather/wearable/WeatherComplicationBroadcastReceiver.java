package com.thewizrd.simpleweather.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherComplicationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            // Request a full update
            WearableDataListenerService.enqueueWork(context,
                    new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_REQUESTSETTINGSUPDATE));
            WearableDataListenerService.enqueueWork(context,
                    new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_REQUESTLOCATIONUPDATE));
            WearableDataListenerService.enqueueWork(context,
                    new Intent(context, WearableDataListenerService.class)
                            .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE)
                            .putExtra(WearableDataListenerService.EXTRA_FORCEUPDATE, true));
            return;
        }

        // Relay intent to WeatherComplicationIntentService
        intent.setClass(context, WeatherComplicationIntentService.class);
        WeatherComplicationIntentService.enqueueWork(context, intent);
    }
}
