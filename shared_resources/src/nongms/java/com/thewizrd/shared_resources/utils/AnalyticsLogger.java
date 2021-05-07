package com.thewizrd.shared_resources.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

public class AnalyticsLogger {
    public static void logEvent(@NonNull @Size(min = 1L, max = 40L) String eventName) {
        logEvent(eventName, null);
    }

    public static void logEvent(@NonNull @Size(min = 1L, max = 40L) String eventName, @Nullable Bundle properties) {
        String append = properties == null ? "" : StringUtils.lineSeparator() + properties.toString();
        Logger.writeLine(Log.INFO, "EVENT | " + eventName + append);
    }
}
