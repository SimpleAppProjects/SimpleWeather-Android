package com.thewizrd.simpleweather.wearable

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WearableWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private var mPhoneNodeWithApp: Node? = null

    companion object {
        private const val TAG = "WearableWorker"

        // Actions
        private const val KEY_ACTION = "action"
        private const val KEY_FORCEREFRESH = "refresh"
        const val ACTION_REQUESTUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_UPDATE"
        const val ACTION_REQUESTSETTINGSUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_SETTINGS_UPDATE"
        const val ACTION_REQUESTLOCATIONUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_LOCATION_UPDATE"
        const val ACTION_REQUESTWEATHERUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_WEATHER_UPDATE"

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, forceRefresh: Boolean = false) {
            when (intentAction) {
                ACTION_REQUESTUPDATE,
                ACTION_REQUESTSETTINGSUPDATE,
                ACTION_REQUESTLOCATIONUPDATE,
                ACTION_REQUESTWEATHERUPDATE -> {
                    startWork(context.applicationContext, intentAction, forceRefresh)
                }
            }
        }

        private fun startWork(context: Context, intentAction: String, forceRefresh: Boolean) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .build()

            val updateRequest = OneTimeWorkRequest.Builder(WearableWorker::class.java)
                .setConstraints(constraints)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, intentAction)
                        .putBoolean(KEY_FORCEREFRESH, forceRefresh)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }

        /*
         * There should only ever be one phone in a node set (much less w/ the correct capability), so
         * I am just grabbing the first one (which should be the only one).
         */
        private fun pickBestNodeId(nodes: Collection<Node>): Node? {
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

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        val intentAction = inputData.getString(KEY_ACTION)
        val forceRefresh = inputData.getBoolean(KEY_FORCEREFRESH, false)

        Logger.writeLine(Log.INFO, "%s: Action: %s; forceRefresh: %s", TAG, intentAction, forceRefresh)

        // Check if nodes are available
        mPhoneNodeWithApp = checkIfPhoneHasApp()

        if (mPhoneNodeWithApp != null) {
            when (intentAction) {
                ACTION_REQUESTUPDATE -> {
                    verifySettingsData(forceRefresh)
                    verifyLocationData(forceRefresh)
                    verifyWeatherData(forceRefresh)
                }
                ACTION_REQUESTSETTINGSUPDATE -> {
                    verifySettingsData(forceRefresh)
                }
                ACTION_REQUESTLOCATIONUPDATE -> {
                    verifyLocationData(forceRefresh)
                }
                ACTION_REQUESTWEATHERUPDATE -> {
                    verifyWeatherData(forceRefresh)
                }
            }
        } else {
            LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(Intent(WearableHelper.ErrorPath))
        }

        return Result.success()
    }

    /* Wearable Functions */
    private suspend fun verifySettingsData(forceRefresh: Boolean) {
        withContext(Dispatchers.IO) {
            var dataItem: DataItem? = null

            if (!forceRefresh) {
                try {
                    dataItem = Wearable.getDataClient(applicationContext)
                        .getDataItem(
                            WearableHelper.getWearDataUri(
                                mPhoneNodeWithApp!!.id,
                                WearableHelper.SettingsPath
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    logError(e)
                }
            }

            if (dataItem == null) {
                // Send message to device to get settings
                sendSettingsRequest()
            } else {
                // Update with data
                DataSyncManager.updateSettings(
                    applicationContext,
                    DataMapItem.fromDataItem(dataItem).dataMap
                )
            }
        }
    }

    private suspend fun sendSettingsRequest() {
        withContext(Dispatchers.IO) {
            try {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(mPhoneNodeWithApp!!.id, WearableHelper.SettingsPath, null)
                    .await()
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    private suspend fun verifyLocationData(forceRefresh: Boolean) {
        withContext(Dispatchers.IO) {
            var dataItem: DataItem? = null

            if (!forceRefresh) {
                try {
                    dataItem = Wearable.getDataClient(applicationContext)
                        .getDataItem(
                            WearableHelper.getWearDataUri(
                                mPhoneNodeWithApp!!.id,
                                WearableHelper.LocationPath
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    logError(e)
                }
            }

            if (dataItem == null) {
                // Send message to device to get location data
                sendLocationRequest()
            } else {
                // Update with data
                DataSyncManager.updateLocation(
                    applicationContext,
                    DataMapItem.fromDataItem(dataItem).dataMap
                )
            }
        }
    }

    private suspend fun sendLocationRequest() {
        withContext(Dispatchers.IO) {
            try {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(mPhoneNodeWithApp!!.id, WearableHelper.LocationPath, null)
                    .await()
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    private suspend fun verifyWeatherData(forceRefresh: Boolean) {
        withContext(Dispatchers.IO) {
            var dataItem: DataItem? = null

            if (!forceRefresh) {
                try {
                    dataItem = Wearable.getDataClient(applicationContext)
                        .getDataItem(
                            WearableHelper.getWearDataUri(
                                mPhoneNodeWithApp!!.id,
                                WearableHelper.WeatherPath
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    logError(e)
                }
            }

            if (dataItem == null) {
                // Send message to device to get settings
                sendWeatherRequest()
            } else {
                // Update with data
                DataSyncManager.updateWeather(
                    applicationContext,
                    DataMapItem.fromDataItem(dataItem).dataMap
                )
            }
        }
    }

    private suspend fun sendWeatherRequest() {
        withContext(Dispatchers.IO) {
            // Send message to device to get settings
            try {
                Wearable.getMessageClient(applicationContext)
                    .sendMessage(mPhoneNodeWithApp!!.id, WearableHelper.WeatherPath, null)
                    .await()
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    private suspend fun checkIfPhoneHasApp(): Node? {
        var node: Node? = null

        try {
            val capabilityInfo = Wearable.getCapabilityClient(applicationContext)
                .getCapability(
                    WearableHelper.CAPABILITY_PHONE_APP,
                    CapabilityClient.FILTER_ALL
                )
                .await()
            node = pickBestNodeId(capabilityInfo.nodes)
        } catch (e: Exception) {
            logError(e)
        }

        return node
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
        }

        Logger.writeLine(Log.ERROR, e)
    }
}