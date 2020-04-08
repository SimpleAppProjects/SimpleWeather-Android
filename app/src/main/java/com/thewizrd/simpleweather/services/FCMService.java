package com.thewizrd.simpleweather.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {

            if (remoteMessage.getData().containsKey("invalidate")) {
                if (remoteMessage.getData().containsKey("date")) {
                    try {
                        long date = Long.parseLong(remoteMessage.getData().get("date"));
                        ImageDataHelper.setImageDBUpdateTime(date);
                    } catch (Exception ignored) {
                    }
                }
                // For long-running tasks (10 seconds or more) use WorkManager.
                FCMWorker.enqueueAction(this.getApplicationContext(), FCMWorker.ACTION_INVALIDATE);
            }

        }
    }
}
