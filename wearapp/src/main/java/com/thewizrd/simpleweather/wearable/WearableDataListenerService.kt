package com.thewizrd.simpleweather.wearable

import com.google.android.gms.wearable.*
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.wearable.WearableHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                            GlobalScope.launch(Dispatchers.Default) {
                                DataSyncManager.updateSettings(applicationContext, dataMap)
                            }
                        }
                        WearableHelper.LocationPath -> {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            GlobalScope.launch(Dispatchers.Default) {
                                DataSyncManager.updateLocation(applicationContext, dataMap)
                            }
                        }
                        WearableHelper.WeatherPath -> {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            GlobalScope.launch(Dispatchers.Default) {
                                DataSyncManager.updateWeather(applicationContext, dataMap)
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