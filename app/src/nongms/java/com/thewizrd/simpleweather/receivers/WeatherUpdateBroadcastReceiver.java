package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.services.UpdaterUtils;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Logger.writeLine(Log.DEBUG, "WeatherUpdateBroadcastReceiver: %s", intent.getAction());
            UpdaterUtils.startAlarm(context, true);
        }
    }
}
