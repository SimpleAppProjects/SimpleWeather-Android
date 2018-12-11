package com.thewizrd.simpleweather.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherComplicationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WeatherComplicationIntentService.enqueueWork(context,
                    new Intent(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE)
                            .putExtra(WearableDataListenerService.EXTRA_FORCEUPDATE, true));
            return;
        }

        // Relay intent to WeatherComplicationIntentService
        intent.setClass(context, WeatherComplicationIntentService.class);
        WeatherComplicationIntentService.enqueueWork(context, intent);
    }
}
