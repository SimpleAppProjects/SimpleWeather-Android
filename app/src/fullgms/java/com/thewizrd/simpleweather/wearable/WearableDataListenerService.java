package com.thewizrd.simpleweather.wearable;

import android.content.Intent;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.simpleweather.LaunchActivity;

public class WearableDataListenerService extends WearableListenerService {
    private static final String TAG = "WearableDataListenerService";
    private static final int JOB_ID = 1002;

    private SettingsManager settingsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsManager = new SettingsManager(getApplicationContext());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case WearableHelper.StartActivityPath:
                Intent startIntent = new Intent(this, LaunchActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(startIntent);
                break;
            case WearableHelper.SettingsPath:
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE, true);
                break;
            case WearableHelper.LocationPath:
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDLOCATIONUPDATE, true);
                break;
            case WearableHelper.WeatherPath:
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDWEATHERUPDATE, true);
                break;
            case WearableHelper.IsSetupPath:
                sendSetupStatus(messageEvent.getSourceNodeId());
                break;
        }
    }

    private void sendSetupStatus(String nodeID) {
        Wearable.getMessageClient(this)
                .sendMessage(nodeID, WearableHelper.IsSetupPath,
                        new byte[]{(byte) (settingsManager.isWeatherLoaded() ? 1 : 0)});
    }
}
