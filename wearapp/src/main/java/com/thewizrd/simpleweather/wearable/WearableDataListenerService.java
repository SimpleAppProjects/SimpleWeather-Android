package com.thewizrd.simpleweather.wearable;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;

public class WearableDataListenerService extends WearableListenerService {
    private static final String TAG = "WearableDataListenerService";

    private static boolean acceptDataUpdates = false;

    public static void setAcceptDataUpdates(boolean value) {
        acceptDataUpdates = value;
    }

    private SettingsManager settingsMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsMgr = new SettingsManager(this.getApplicationContext());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        // Only handle data changes if
        // DataSync is on,
        // App hasn't been setup yet,
        // Or if we are setup but want to change location and sync data (SetupSyncActivity)
        if (settingsMgr.getDataSync() != WearableDataSync.OFF || acceptDataUpdates) {
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(WearableHelper.SettingsPath) == 0) {
                        final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                DataSyncManager.updateSettings(getApplicationContext(), dataMap);
                            }
                        });
                    } else if (item.getUri().getPath().compareTo(WearableHelper.LocationPath) == 0) {
                        final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                DataSyncManager.updateLocation(getApplicationContext(), dataMap);
                            }
                        });
                    } else if (item.getUri().getPath().compareTo(WearableHelper.WeatherPath) == 0) {
                        final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                DataSyncManager.updateWeather(getApplicationContext(), dataMap);
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }
}
