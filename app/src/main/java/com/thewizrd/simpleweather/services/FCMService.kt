package com.thewizrd.simpleweather.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper
import com.thewizrd.simpleweather.services.FCMWorker.Companion.enqueueAction

class FCMService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            if (remoteMessage.data.containsKey("invalidate")) {
                if (remoteMessage.data.containsKey("date")) {
                    val date = remoteMessage.data["date"]
                    if (date != null) {
                        ImageDataHelper.setImageDBUpdateTime(date.toLong())
                    }
                }
                // For long-running tasks (10 seconds or more) use WorkManager.
                enqueueAction(this.applicationContext, FCMWorker.ACTION_INVALIDATE)
            }
        }
    }
}