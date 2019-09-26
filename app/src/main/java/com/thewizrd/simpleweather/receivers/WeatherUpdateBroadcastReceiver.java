package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.simpleweather.services.WeatherUpdaterService;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WeatherUpdaterService.enqueueWork(context, new Intent(WeatherUpdaterService.ACTION_UPDATEWEATHER));
            return;
        }

        // Relay intent to WeatherWidgetService
        intent.setClass(context, WeatherUpdaterService.class);
        WeatherUpdaterService.enqueueWork(context, intent);
    }
}
