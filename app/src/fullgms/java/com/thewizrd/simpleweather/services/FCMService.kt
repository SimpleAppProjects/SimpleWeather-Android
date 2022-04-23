package com.thewizrd.simpleweather.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thewizrd.simpleweather.images.imageDataService

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
                        imageDataService.setImageDBUpdateTime(date.toLong())
                    }
                }
                // For long-running tasks (10 seconds or more) use WorkManager.
                FCMWorker.enqueueAction(this.applicationContext, FCMWorker.ACTION_INVALIDATE)
            }
        }
    }
}