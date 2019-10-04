package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_STARTALARM);
        }
    }
}
