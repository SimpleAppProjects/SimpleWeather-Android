package com.thewizrd.simpleweather.wearable;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.wearable.WearableHelper;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class WearableWorker extends Worker {
    private static final String TAG = "WearableWorker";

    // Actions
    private static final String KEY_ACTION = "action";
    private static final String KEY_FORCEREFRESH = "refresh";
    public static final String ACTION_REQUESTUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_UPDATE";
    public static final String ACTION_REQUESTSETTINGSUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_SETTINGS_UPDATE";
    public static final String ACTION_REQUESTLOCATIONUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_LOCATION_UPDATE";
    public static final String ACTION_REQUESTWEATHERUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_WEATHER_UPDATE";

    private final Context mContext;
    private Node mPhoneNodeWithApp;

    public WearableWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        enqueueAction(context, intentAction, false);
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction, boolean forceRefresh) {
        context = context.getApplicationContext();

        switch (intentAction) {
            case ACTION_REQUESTUPDATE:
            case ACTION_REQUESTSETTINGSUPDATE:
            case ACTION_REQUESTLOCATIONUPDATE:
            case ACTION_REQUESTWEATHERUPDATE:
                startWork(context, intentAction, forceRefresh);
                break;
        }
    }

    private static void startWork(@NonNull Context context, @NonNull String intentAction, boolean forceRefesh) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WearableWorker.class)
                .setConstraints(constraints)
                .setInputData(
                        new Data.Builder()
                                .putString(KEY_ACTION, intentAction)
                                .putBoolean(KEY_FORCEREFRESH, forceRefesh)
                                .build()
                )
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(String.format(Locale.ROOT, "%s:%s_oneTime", TAG, intentAction), ExistingWorkPolicy.APPEND_OR_REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        final String intentAction = getInputData().getString(KEY_ACTION);
        final boolean forceRefresh = getInputData().getBoolean(KEY_FORCEREFRESH, false);

        Logger.writeLine(Log.INFO, "%s: Action: %s; forceRefresh: %s", TAG, intentAction, forceRefresh);

        // Check if nodes are available
        mPhoneNodeWithApp = checkIfPhoneHasApp();

        if (mPhoneNodeWithApp != null) {
            if (ACTION_REQUESTUPDATE.equals(intentAction)) {
                verifySettingsData(forceRefresh);
                verifyLocationData(forceRefresh);
                verifyWeatherData(forceRefresh);
            } else if (ACTION_REQUESTSETTINGSUPDATE.equals(intentAction)) {
                verifySettingsData(forceRefresh);
            } else if (ACTION_REQUESTLOCATIONUPDATE.equals(intentAction)) {
                verifyLocationData(forceRefresh);
            } else if (ACTION_REQUESTWEATHERUPDATE.equals(intentAction)) {
                verifyWeatherData(forceRefresh);
            }
        } else {
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(new Intent(WearableHelper.ErrorPath));
        }

        return Result.success();
    }

    /* Wearable Functions */
    @WorkerThread
    private void verifySettingsData(boolean forceRefresh) {
        DataItem dataItem = null;

        if (!forceRefresh) {
            try {
                dataItem = Tasks.await(Wearable.getDataClient(mContext)
                        .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath)));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        if (dataItem == null) {
            // Send message to device to get settings
            sendSettingsRequest();
        } else {
            // Update with data
            DataSyncManager.updateSettings(mContext, DataMapItem.fromDataItem(dataItem).getDataMap());
        }
    }

    @WorkerThread
    private void sendSettingsRequest() {
        try {
            Tasks.await(Wearable.getMessageClient(mContext)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath, null));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                // Ignore this error
                if (apiException.getStatusCode() != WearableStatusCodes.API_NOT_CONNECTED &&
                        apiException.getStatusCode() != WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e);
                }
            } else {
                Logger.writeLine(Log.ERROR, e);
            }
        }
    }

    @WorkerThread
    private void verifyLocationData(boolean forceRefresh) {
        DataItem dataItem = null;

        if (!forceRefresh) {
            try {
                dataItem = Tasks.await(Wearable.getDataClient(mContext)
                        .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath)));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        if (dataItem == null) {
            // Send message to device to get location data
            sendLocationRequest();
        } else {
            // Update with data
            DataSyncManager.updateLocation(mContext, DataMapItem.fromDataItem(dataItem).getDataMap());
        }
    }

    @WorkerThread
    private void sendLocationRequest() {
        try {
            Tasks.await(Wearable.getMessageClient(mContext)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath, null));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                // Ignore this error
                if (apiException.getStatusCode() != WearableStatusCodes.API_NOT_CONNECTED &&
                        apiException.getStatusCode() != WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e);
                }
            } else {
                Logger.writeLine(Log.ERROR, e);
            }
        }
    }

    @WorkerThread
    private void verifyWeatherData(boolean forceRefresh) {
        DataItem dataItem = null;

        if (!forceRefresh) {
            try {
                dataItem = Tasks.await(Wearable.getDataClient(mContext)
                        .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath)));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        if (dataItem == null) {
            // Send message to device to get settings
            sendWeatherRequest();
        } else {
            // Update with data
            DataSyncManager.updateWeather(mContext, DataMapItem.fromDataItem(dataItem).getDataMap());
        }
    }

    @WorkerThread
    private void sendWeatherRequest() {
        // Send message to device to get settings
        try {
            Tasks.await(Wearable.getMessageClient(mContext)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath, null));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                // Ignore this error
                if (apiException.getStatusCode() != WearableStatusCodes.API_NOT_CONNECTED &&
                        apiException.getStatusCode() != WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e);
                }
            } else {
                Logger.writeLine(Log.ERROR, e);
            }
        }
    }

    private Node checkIfPhoneHasApp() {
        Node node = null;

        try {
            CapabilityInfo capabilityInfo = Tasks.await(Wearable.getCapabilityClient(mContext)
                    .getCapability(WearableHelper.CAPABILITY_PHONE_APP,
                            CapabilityClient.FILTER_ALL));
            node = pickBestNodeId(capabilityInfo.getNodes());
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        return node;
    }

    /*
     * There should only ever be one phone in a node set (much less w/ the correct capability), so
     * I am just grabbing the first one (which should be the only one).
     */
    private static Node pickBestNodeId(Collection<Node> nodes) {
        Node bestNode = null;

        // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node;
            }
            bestNode = node;
        }
        return bestNode;
    }
}
