package com.thewizrd.simpleweather.wearable

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.CapabilityClient.OnCapabilityChangedListener
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.helpers.showConfirmationOverlay
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class WearableListenerActivity : UserLocaleActivity(), OnMessageReceivedListener, OnCapabilityChangedListener {
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

    @Volatile
    protected var mPhoneNodeWithApp: Node? = null
    private var mConnectionStatus = WearConnectionStatus.CONNECTING
    var isAcceptingDataUpdates = false

    protected val settingsManager: SettingsManager = App.instance.settingsManager

    protected abstract val broadcastReceiver: BroadcastReceiver
    protected abstract val intentFilter: IntentFilter

    protected lateinit var remoteActivityHelper: RemoteActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteActivityHelper = RemoteActivityHelper(this)
    }

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

    protected fun openAppOnPhone(showAnimation: Boolean = true) {
        lifecycleScope.launch {
            connect()

            if (mPhoneNodeWithApp == null) {
                Toast.makeText(
                    this@WearableListenerActivity,
                    R.string.status_node_unavailable,
                    Toast.LENGTH_SHORT
                ).show()

                when (PhoneTypeHelper.getPhoneDeviceType(this@WearableListenerActivity)) {
                    PhoneTypeHelper.DEVICE_TYPE_ANDROID -> {
                        val intentAndroid = Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(PlayStoreUtils.getPlayStoreURI())

                        runCatching {
                            remoteActivityHelper.startRemoteActivity(intentAndroid)
                                .await()

                            showConfirmationOverlay(true)
                        }.onFailure {
                            if (it !is CancellationException) {
                                showConfirmationOverlay(false)
                            }
                        }
                    }
                    else -> {
                        Toast.makeText(
                            this@WearableListenerActivity,
                            R.string.status_node_notsupported,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                // Send message to device to start activity
                val result = sendMessage(
                    mPhoneNodeWithApp!!.id,
                    WearableHelper.StartActivityPath,
                    ByteArray(0)
                )

                val success = result != -1

                if (showAnimation) {
                    launch(Dispatchers.Main) {
                        ConfirmationOverlay()
                            .setType(if (success) ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION else ConfirmationOverlay.FAILURE_ANIMATION)
                            .showOn(this@WearableListenerActivity)
                    }
                }
            }
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
            val connectedNodes = getConnectedNodes()
            mPhoneNodeWithApp = pickBestNodeId(capabilityInfo.nodes)

            if (mPhoneNodeWithApp == null) {
                /*
                 * If a device is disconnected from the wear network, capable nodes are empty
                 *
                 * No capable nodes can mean the app is not installed on the remote device or the
                 * device is disconnected.
                 *
                 * Verify if we're connected to any nodes; if not, we're truly disconnected
                 */
                mConnectionStatus = if (connectedNodes.isNullOrEmpty()) {
                    WearConnectionStatus.DISCONNECTED
                } else {
                    WearConnectionStatus.APPNOTINSTALLED
                }
            } else {
                if (mPhoneNodeWithApp!!.isNearby && connectedNodes.any { it.id == mPhoneNodeWithApp!!.id }) {
                    mConnectionStatus = WearConnectionStatus.CONNECTED
                } else {
                    try {
                        sendPing(mPhoneNodeWithApp!!.id)
                        mConnectionStatus = WearConnectionStatus.CONNECTED
                    } catch (e: ApiException) {
                        if (e.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                            mConnectionStatus = WearConnectionStatus.DISCONNECTED
                        } else {
                            Logger.writeLine(Log.ERROR, e)
                        }
                    }
                }
            }

            LocalBroadcastManager.getInstance(this@WearableListenerActivity)
                .sendBroadcast(
                    Intent(ACTION_UPDATECONNECTIONSTATUS)
                        .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value)
                )
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
        } catch (e: Exception) {
            logError(e)
        }
    }

    protected suspend fun updateConnectionStatus() {
        checkConnectionStatus()

        LocalBroadcastManager.getInstance(this@WearableListenerActivity)
                .sendBroadcast(Intent(ACTION_UPDATECONNECTIONSTATUS)
                        .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value))
    }

    protected suspend fun checkConnectionStatus() {
        val connectedNodes = getConnectedNodes()
        mPhoneNodeWithApp = checkIfPhoneHasApp()

        if (mPhoneNodeWithApp == null) {
            /*
             * If a device is disconnected from the wear network, capable nodes are empty
             *
             * No capable nodes can mean the app is not installed on the remote device or the
             * device is disconnected.
             *
             * Verify if we're connected to any nodes; if not, we're truly disconnected
             */
            mConnectionStatus = if (connectedNodes.isNullOrEmpty()) {
                WearConnectionStatus.DISCONNECTED
            } else {
                WearConnectionStatus.APPNOTINSTALLED
            }
        } else {
            if (mPhoneNodeWithApp!!.isNearby && connectedNodes.any { it.id == mPhoneNodeWithApp!!.id }) {
                mConnectionStatus = WearConnectionStatus.CONNECTED
            } else {
                try {
                    sendPing(mPhoneNodeWithApp!!.id)
                    mConnectionStatus = WearConnectionStatus.CONNECTED
                } catch (e: ApiException) {
                    if (e.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                        mConnectionStatus = WearConnectionStatus.DISCONNECTED
                    } else {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }
            }
        }
    }

    suspend fun getConnectionStatus(): WearConnectionStatus {
        checkConnectionStatus()
        return mConnectionStatus
    }

    protected suspend fun checkIfPhoneHasApp(): Node? = withContext(Dispatchers.IO) {
        var node: Node? = null
        try {
            val capabilityInfo = Wearable.getCapabilityClient(this@WearableListenerActivity)
                .getCapability(
                    WearableHelper.CAPABILITY_PHONE_APP,
                    CapabilityClient.FILTER_ALL
                )
                .await()
            node = pickBestNodeId(capabilityInfo.nodes)
        } catch (e: Exception) {
            logError(e)
        }
        return@withContext node
    }

    protected suspend fun connect(): Boolean {
        if (mPhoneNodeWithApp == null)
            mPhoneNodeWithApp = checkIfPhoneHasApp()

        return mPhoneNodeWithApp != null
    }

    private suspend fun getConnectedNodes(): List<Node> {
        try {
            return Wearable.getNodeClient(this)
                .connectedNodes
                .await()
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
        }

        return emptyList()
    }

    protected suspend fun sendMessage(nodeID: String, path: String, data: ByteArray?): Int? {
        try {
            return Wearable.getMessageClient(this@WearableListenerActivity)
                .sendMessage(nodeID, path, data).await()
        } catch (e: Exception) {
            if (e is ApiException || e.cause is ApiException) {
                val apiException = e.cause as? ApiException ?: e as? ApiException
                if (apiException?.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED) {
                    mConnectionStatus = WearConnectionStatus.DISCONNECTED

                    LocalBroadcastManager.getInstance(this@WearableListenerActivity)
                        .sendBroadcast(
                            Intent(ACTION_UPDATECONNECTIONSTATUS)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value)
                        )
                }
            }

            Logger.writeLine(Log.ERROR, e)
        }

        return -1
    }

    @Throws(ApiException::class)
    private suspend fun sendPing(nodeID: String) = withContext(Dispatchers.IO) {
        try {
            Wearable.getMessageClient(this@WearableListenerActivity)
                .sendMessage(nodeID, WearableHelper.PingPath, null)
                .await()
        } catch (e: Exception) {
            if (e is ApiException) {
                throw e
            }
            if (e.cause is ApiException) {
                throw e.cause as ApiException
            }
            logError(e)
        }
    }

    private fun logError(e: Exception) {
        if (e is ApiException || e.cause is ApiException) {
            val apiException = e.cause as? ApiException ?: e as? ApiException
            if (apiException?.statusCode == WearableStatusCodes.API_NOT_CONNECTED ||
                apiException?.statusCode == WearableStatusCodes.TARGET_NODE_NOT_CONNECTED
            ) {
                // Ignore this error
                return
            }
        } else if (e is CancellationException || e is InterruptedException) {
            // Ignore this error
            return
        }

        Timber.e(e)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    protected fun setConnectionStatus(status: WearConnectionStatus) {
        mConnectionStatus = status

        LocalBroadcastManager.getInstance(this@WearableListenerActivity)
            .sendBroadcast(
                Intent(ACTION_UPDATECONNECTIONSTATUS)
                    .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.value)
            )
    }
}