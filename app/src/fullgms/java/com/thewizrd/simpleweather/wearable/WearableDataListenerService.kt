package com.thewizrd.simpleweather.wearable

import android.content.Intent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.simpleweather.LaunchActivity

class WearableDataListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "WearableDataListenerService"
        private const val JOB_ID = 1002
    }

    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(applicationContext)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        super.onDataChanged(dataEventBuffer)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearableHelper.StartActivityPath -> {
                val startIntent = Intent(this, LaunchActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(startIntent)
            }
            WearableHelper.SettingsPath -> {
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
            }
            WearableHelper.LocationPath -> {
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDLOCATIONUPDATE)
            }
            WearableHelper.WeatherPath -> {
                WearableWorker.enqueueAction(this, WearableWorkerActions.ACTION_SENDWEATHERUPDATE)
            }
            WearableHelper.IsSetupPath -> {
                WearableWorker.sendSetupStatus(this, messageEvent.sourceNodeId)
            }
        }
    }
}