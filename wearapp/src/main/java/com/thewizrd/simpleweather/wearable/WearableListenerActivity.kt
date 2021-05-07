package com.thewizrd.simpleweather.wearable

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.support.wearable.phone.PhoneDeviceType
import android.support.wearable.view.ConfirmationOverlay
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.CapabilityClient.OnCapabilityChangedListener
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import com.google.android.wearable.intent.RemoteIntent
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutionException

abstract class WearableListenerActivity : UserLocaleActivity(), OnMessageReceivedListener, OnCapabilityChangedListener {
    protected var mPhoneNodeWithApp: Node? = null
    protected var mConnectionStatus = WearConnectionStatus.DISCONNECTED
    var isAcceptingDataUpdates = false

    protected val settingsManager: SettingsManager = App.instance.settingsManager

    companion object {
        // Actions
        const val ACTION_OPENONPHONE = "SimpleWeather.Droid.Wear.action.OPEN_APP_ON_PHONE"
        const val ACTION_SHOWSTORELISTING = "SimpleWeather.Droid.Wear.action.SHOW_STORE_LISTING"
        const val ACTION_SENDCONNECTIONSTATUS = "SimpleWeather.Droid.Wear.action.SEND_CONNECTION_STATUS"
        const val ACTION_UPDATECONNECTIONSTATUS = "SimpleWeather.Droid.Wear.action.UPDATE_CONNECTION_STATUS"
        const val ACTION_REQUESTSETUPSTATUS = "SimpleWeather.Droid.Wear.action.REQUEST_SETUP_STATUS"

        // Extras
        /**
         * Extra contains success flag for open on phone action.
         *
         * @see .ACTION_OPENONPHONE
         */
        const val EXTRA_SUCCESS = "SimpleWeather.Droid.Wear.extra.SUCCESS"

        /**
         * Extra contains flag for whether or not to show the animation for the open on phone action.
         *
         * @see .ACTION_OPENONPHONE
         */
        const val EXTRA_SHOWANIMATION = "SimpleWeather.Droid.Wear.extra.SHOW_ANIMATION"

        /**
         * Extra contains connection status for WearOS device and connected phone
         *
         * @see WearConnectionStatus
         *
         * @see WearableListenerActivity
         */
        const val EXTRA_CONNECTIONSTATUS = "SimpleWeather.Droid.Wear.extra.CONNECTION_STATUS"
        const val EXTRA_DEVICESETUPSTATUS = "SimpleWeather.Droid.Wear.extra.DEVICE_SETUP_STATUS"

        /*
         * There should only ever be one phone in a node set (much less w/ the correct capability), so
         * I am just grabbing the first one (which should be the only one).
         */
        protected fun pickBestNodeId(nodes: Collection<Node>): Node? {
            var bestNode: Node? = null

            // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
            for (node in nodes) {
                if (node.isNearby) {
                    return node
                }
                bestNode = node
            }
            return bestNode
        }
    }

    protected abstract val broadcastReceiver: BroadcastReceiver
    protected abstract val intentFilter: IntentFilter

    override fun onStart() {
        Wearable.getCapabilityClient(this).addListener(this, WearableHelper.CAPABILITY_PHONE_APP)
        Wearable.getMessageClient(this).addListener(this)

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, intentFilter)

        lifecycleScope.launch { checkConnectionStatus() }

        // Register listeners before fragments are started
        super.onStart()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver)
        Wearable.getCapabilityClient(this).removeListener(this, WearableHelper.CAPABILITY_PHONE_APP)
        Wearable.getMessageClient(this).removeListener(this)
        super.onPause()
    }

    protected suspend fun openAppOnPhone(showAnimation: Boolean = true) = withContext(Dispatchers.Default) {
        connect()

        if (mPhoneNodeWithApp == null) {
            launch(Dispatchers.Main) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    Toast.makeText(this@WearableListenerActivity, R.string.status_node_unavailable, Toast.LENGTH_SHORT).show()
                }
            }

            val deviceType = PhoneDeviceType.getPhoneDeviceType(this@WearableListenerActivity)
            when (deviceType) {
                PhoneDeviceType.DEVICE_TYPE_ANDROID -> {
                    val intentAndroid = Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(PlayStoreUtils.getPlayStoreURI())

                    RemoteIntent.startRemoteActivity(this@WearableListenerActivity, intentAndroid,
                            ConfirmationResultReceiver(this@WearableListenerActivity))
                }
                PhoneDeviceType.DEVICE_TYPE_IOS -> {
                    launch(Dispatchers.Main) {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Toast.makeText(this@WearableListenerActivity, R.string.status_node_notsupported, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    launch(Dispatchers.Main) {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            Toast.makeText(this@WearableListenerActivity, R.string.status_node_notsupported, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            false
        } else {
            // Send message to device to start activity
            var result = -1
            try {
                result = withContext(Dispatchers.IO) {
                    Wearable.getMessageClient(this@WearableListenerActivity)
                            .sendMessage(mPhoneNodeWithApp!!.id, WearableHelper.StartActivityPath, ByteArray(0))
                            .await()
                }
            } catch (e: ExecutionException) {
                Timber.e(e)
            } catch (e: InterruptedException) {
                Timber.d(e)
            }

            val success = result != -1

            if (showAnimation) {
                launch(Dispatchers.Main) {
                    ConfirmationOverlay()
                            .setType(if (success) ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION else ConfirmationOverlay.FAILURE_ANIMATION)
                            .showOn(this@WearableListenerActivity)
                }
            }

            success
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (settingsManager.getDataSync() != WearableDataSync.OFF || isAcceptingDataUpdates) {
            if (messageEvent.path == WearableHelper.IsSetupPath) {
                val data = messageEvent.data
                val isDeviceSetup = data[0] != 0.toByte()
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(Intent(WearableHelper.IsSetupPath)
                                .putExtra(EXTRA_DEVICESETUPSTATUS, isDeviceSetup)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value))
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        lifecycleScope.launch(Dispatchers.Default) {
            mPhoneNodeWithApp = pickBestNodeId(capabilityInfo.nodes)

            if (mPhoneNodeWithApp == null) {
                mConnectionStatus = WearConnectionStatus.DISCONNECTED
            } else {
                if (mPhoneNodeWithApp!!.isNearby) {
                    mConnectionStatus = WearConnectionStatus.CONNECTED
                } else {
                    try {
                        sendPing(mPhoneNodeWithApp!!.id)
                        mConnectionStatus = WearConnectionStatus.CONNECTED
                    } catch (e: ApiException) {
                        if (e.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                            mConnectionStatus = WearConnectionStatus.DISCONNECTED
                        }
                    }
                }
            }

            LocalBroadcastManager.getInstance(this@WearableListenerActivity)
                    .sendBroadcast(Intent(ACTION_UPDATECONNECTIONSTATUS)
                            .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value))
        }
    }

    protected suspend fun sendSetupStatusRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(this@WearableListenerActivity).sendBroadcast(
                    Intent(WearableHelper.ErrorPath))
            return
        }

        try {
            Wearable.getMessageClient(this@WearableListenerActivity)
                    .sendMessage(mPhoneNodeWithApp!!.id, WearableHelper.IsSetupPath, ByteArray(0))
                    .await()
        } catch (e: ExecutionException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            Timber.d(e)
        }
    }

    protected suspend fun updateConnectionStatus() {
        checkConnectionStatus()

        LocalBroadcastManager.getInstance(this@WearableListenerActivity)
                .sendBroadcast(Intent(ACTION_UPDATECONNECTIONSTATUS)
                        .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value))
    }

    protected suspend fun checkConnectionStatus() {
        mPhoneNodeWithApp = checkIfPhoneHasApp()
        if (mPhoneNodeWithApp == null) {
            mConnectionStatus = WearConnectionStatus.DISCONNECTED
        } else {
            if (mPhoneNodeWithApp!!.isNearby) {
                mConnectionStatus = WearConnectionStatus.CONNECTED
            } else {
                try {
                    sendPing(mPhoneNodeWithApp!!.id)
                    mConnectionStatus = WearConnectionStatus.CONNECTED
                } catch (e: ApiException) {
                    if (e.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                        mConnectionStatus = WearConnectionStatus.DISCONNECTED
                    }
                }
            }
        }
    }

    protected suspend fun checkIfPhoneHasApp(): Node? = withContext(Dispatchers.IO) {
        var node: Node? = null
        try {
            val capabilityInfo = Wearable.getCapabilityClient(this@WearableListenerActivity)
                    .getCapability(WearableHelper.CAPABILITY_PHONE_APP,
                            CapabilityClient.FILTER_ALL)
                    .await()
            node = pickBestNodeId(capabilityInfo.nodes)
        } catch (e: ExecutionException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            Timber.d(e)
        }
        return@withContext node
    }

    protected suspend fun connect(): Boolean {
        if (mPhoneNodeWithApp == null)
            mPhoneNodeWithApp = checkIfPhoneHasApp()

        return mPhoneNodeWithApp != null
    }

    @Throws(ApiException::class)
    private suspend fun sendPing(nodeID: String) = withContext(Dispatchers.IO) {
        try {
            Wearable.getMessageClient(this@WearableListenerActivity)
                    .sendMessage(nodeID, WearableHelper.PingPath, null)
                    .await()
        } catch (ex: ExecutionException) {
            if (ex.cause is ApiException) {
                throw ex.cause as ApiException
            }
            Timber.e(ex)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}