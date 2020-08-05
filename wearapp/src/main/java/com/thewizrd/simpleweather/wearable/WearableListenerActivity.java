package com.thewizrd.simpleweather.wearable;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.support.wearable.phone.PhoneDeviceType;
import android.support.wearable.view.ConfirmationOverlay;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public abstract class WearableListenerActivity extends FragmentActivity implements MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener {
    protected Node mPhoneNodeWithApp;
    protected WearConnectionStatus mConnectionStatus = WearConnectionStatus.DISCONNECTED;
    private boolean acceptDataUpdates = false;

    // Actions
    public static final String ACTION_OPENONPHONE = "SimpleWeather.Droid.Wear.action.OPEN_APP_ON_PHONE";
    public static final String ACTION_SHOWSTORELISTING = "SimpleWeather.Droid.Wear.action.SHOW_STORE_LISTING";
    public static final String ACTION_SENDCONNECTIONSTATUS = "SimpleWeather.Droid.Wear.action.SEND_CONNECTION_STATUS";
    public static final String ACTION_UPDATECONNECTIONSTATUS = "SimpleWeather.Droid.Wear.action.UPDATE_CONNECTION_STATUS";
    public static final String ACTION_REQUESTSETUPSTATUS = "SimpleWeather.Droid.Wear.action.REQUEST_SETUP_STATUS";

    // Extras
    /**
     * Extra contains success flag for open on phone action.
     *
     * @see #ACTION_OPENONPHONE
     */
    public static final String EXTRA_SUCCESS = "SimpleWeather.Droid.Wear.extra.SUCCESS";
    /**
     * Extra contains flag for whether or not to show the animation for the open on phone action.
     *
     * @see #ACTION_OPENONPHONE
     */
    public static final String EXTRA_SHOWANIMATION = "SimpleWeather.Droid.Wear.extra.SHOW_ANIMATION";
    /**
     * Extra contains connection status for WearOS device and connected phone
     *
     * @see WearConnectionStatus
     * @see WearableListenerActivity
     */
    public static final String EXTRA_CONNECTIONSTATUS = "SimpleWeather.Droid.Wear.extra.CONNECTION_STATUS";
    public static final String EXTRA_DEVICESETUPSTATUS = "SimpleWeather.Droid.Wear.extra.DEVICE_SETUP_STATUS";

    public final boolean isAcceptingDataUpdates() {
        return acceptDataUpdates;
    }

    public final void setAcceptDataUpdates(boolean value) {
        acceptDataUpdates = value;
    }

    protected abstract BroadcastReceiver getBroadcastReceiver();

    protected abstract IntentFilter getIntentFilter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        Wearable.getCapabilityClient(this).addListener(this, WearableHelper.CAPABILITY_PHONE_APP);
        Wearable.getMessageClient(this).addListener(this);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(getBroadcastReceiver(), getIntentFilter());

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                checkConnectionStatus();
            }
        });

        // Register listeners before fragments are started
        super.onStart();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(getBroadcastReceiver());
        Wearable.getCapabilityClient(this).removeListener(this, WearableHelper.CAPABILITY_PHONE_APP);
        Wearable.getMessageClient(this).removeListener(this);
        super.onPause();
    }

    @WorkerThread
    protected boolean openAppOnPhone() {
        return openAppOnPhone(true);
    }

    @WorkerThread
    protected boolean openAppOnPhone(final boolean showAnimation) {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                connect();

                if (mPhoneNodeWithApp == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                                Toast.makeText(WearableListenerActivity.this, R.string.status_node_unavailable, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    int deviceType = PhoneDeviceType.getPhoneDeviceType(WearableListenerActivity.this);
                    switch (deviceType) {
                        case PhoneDeviceType.DEVICE_TYPE_ANDROID:
                            Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                                    .setData(WearableHelper.getPlayStoreURI());

                            RemoteIntent.startRemoteActivity(WearableListenerActivity.this, intentAndroid,
                                    new ConfirmationResultReceiver(WearableListenerActivity.this));
                            break;
                        case PhoneDeviceType.DEVICE_TYPE_IOS:
                        default:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                                        Toast.makeText(WearableListenerActivity.this, R.string.status_node_notsupported, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            break;
                    }

                    return false;
                } else {
                    // Send message to device to start activity
                    int result = -1;
                    try {
                        result = Tasks.await(Wearable.getMessageClient(WearableListenerActivity.this)
                                .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.StartActivityPath, new byte[0]));
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }

                    if (showAnimation) {
                        new ConfirmationOverlay()
                                .setType(result != -1 ? ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION : ConfirmationOverlay.FAILURE_ANIMATION)
                                .showOn(WearableListenerActivity.this);
                    }

                    return result != -1;
                }
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (Settings.getDataSync() != WearableDataSync.OFF || acceptDataUpdates) {
            if (messageEvent.getPath().equals(WearableHelper.IsSetupPath)) {
                byte[] data = messageEvent.getData();
                boolean isDeviceSetup = !(data[0] == 0);
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(new Intent(WearableHelper.IsSetupPath)
                                .putExtra(EXTRA_DEVICESETUPSTATUS, isDeviceSetup)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
            }
        }
    }

    @Override
    public void onCapabilityChanged(@NonNull final CapabilityInfo capabilityInfo) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                mPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());

                if (mPhoneNodeWithApp == null) {
                    mConnectionStatus = WearConnectionStatus.DISCONNECTED;
                } else {
                    if (mPhoneNodeWithApp.isNearby()) {
                        mConnectionStatus = WearConnectionStatus.CONNECTED;
                    } else {
                        try {
                            sendPing(mPhoneNodeWithApp.getId());
                            mConnectionStatus = WearConnectionStatus.CONNECTED;
                        } catch (ApiException e) {
                            if (e.getStatusCode() == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                                mConnectionStatus = WearConnectionStatus.DISCONNECTED;
                            }
                        }
                    }
                }

                LocalBroadcastManager.getInstance(WearableListenerActivity.this)
                        .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
            }
        });
    }

    protected final void sendSetupStatusRequest() {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableListenerActivity.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return;
                }

                try {
                    Tasks.await(Wearable.getMessageClient(WearableListenerActivity.this)
                            .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.IsSetupPath, new byte[0]));
                } catch (ExecutionException | InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e);
                }
            }
        });
    }

    @WorkerThread
    protected void updateConnectionStatus() {
        // Make sure we're not on the main thread
        if (Looper.getMainLooper() == Looper.myLooper())
            throw new IllegalStateException("This task should not be called on the main thread");

        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                checkConnectionStatus();

                LocalBroadcastManager.getInstance(WearableListenerActivity.this)
                        .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
                return null;
            }
        });
    }

    protected void checkConnectionStatus() {
        mPhoneNodeWithApp = checkIfPhoneHasApp();

        if (mPhoneNodeWithApp == null) {
            mConnectionStatus = WearConnectionStatus.DISCONNECTED;
        } else {
            if (mPhoneNodeWithApp.isNearby()) {
                mConnectionStatus = WearConnectionStatus.CONNECTED;
            } else {
                try {
                    sendPing(mPhoneNodeWithApp.getId());
                    mConnectionStatus = WearConnectionStatus.CONNECTED;
                } catch (ApiException e) {
                    if (e.getStatusCode() == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                        mConnectionStatus = WearConnectionStatus.DISCONNECTED;
                    }
                }
            }
        }
    }

    protected final Node checkIfPhoneHasApp() {
        Node node = null;

        try {
            CapabilityInfo capabilityInfo = Tasks.await(Wearable.getCapabilityClient(this)
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
    protected static Node pickBestNodeId(Collection<Node> nodes) {
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

    protected boolean connect() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                if (mPhoneNodeWithApp == null)
                    mPhoneNodeWithApp = checkIfPhoneHasApp();

                return mPhoneNodeWithApp != null;
            }
        });
    }

    private void sendPing(String nodeID) throws ApiException {
        try {
            Tasks.await(Wearable.getMessageClient(this).sendMessage(nodeID, WearableHelper.PingPath, null));
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof ApiException) {
                throw (ApiException) ex.getCause();
            }
            Logger.writeLine(Log.ERROR, ex);
        } catch (Exception e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }
}
