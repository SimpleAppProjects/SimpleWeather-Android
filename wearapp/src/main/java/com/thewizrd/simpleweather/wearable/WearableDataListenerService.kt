package com.thewizrd.simpleweather.wearable

import android.util.Log
import com.google.android.gms.wearable.*
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableDataSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class WearableDataListenerService : WearableListenerService() {
    private lateinit var settingsMgr: SettingsManager

    companion object {
        private const val TAG = "WearableDataListenerService"

        private var acceptDataUpdates = false

        @JvmStatic
        fun setAcceptDataUpdates(value: Boolean) {
            acceptDataUpdates = value
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsMgr = SettingsManager(this.applicationContext)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        // Only handle data changes if
        // DataSync is on,
        // App hasn't been setup yet,
        // Or if we are setup but want to change location and sync data (SetupSyncActivity)
        if (settingsMgr.getDataSync() != WearableDataSync.OFF || acceptDataUpdates) {
            for (event in dataEventBuffer) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val item = event.dataItem
                    when (item.uri.path) {
                        WearableHelper.SettingsPath -> {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            appLib.appScope.launch(Dispatchers.Default) {
                                runCatching {
                                    supervisorScope {
                                        withTimeout(15000) {
                                            DataSyncManager.updateSettings(
                                                applicationContext,
                                                dataMap
                                            )
                                        }
                                    }
                                }.onFailure {
                                    Logger.writeLine(Log.ERROR, "DataSync: settings error", it)
                                }
                            }
                        }
                        WearableHelper.LocationPath -> {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            appLib.appScope.launch(Dispatchers.Default) {
                                runCatching {
                                    supervisorScope {
                                        withTimeout(15000) {
                                            DataSyncManager.updateLocation(
                                                applicationContext,
                                                dataMap
                                            )
                                        }
                                    }
                                }.onFailure {
                                    Logger.writeLine(Log.ERROR, "DataSync: location error", it)
                                }
                            }
                        }
                        WearableHelper.WeatherPath -> {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            appLib.appScope.launch(Dispatchers.Default) {
                                runCatching {
                                    supervisorScope {
                                        withTimeout(15000) {
                                            DataSyncManager.updateWeather(
                                                applicationContext,
                                                dataMap
                                            )
                                        }
                                    }
                                }.onFailure {
                                    Logger.writeLine(Log.ERROR, "DataSync: weather error", it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
    }
}