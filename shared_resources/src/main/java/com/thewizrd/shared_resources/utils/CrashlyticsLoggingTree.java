package com.thewizrd.shared_resources.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import timber.log.Timber;

public class CrashlyticsLoggingTree extends Timber.Tree {
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_TAG = "tag";
    private static final String KEY_MESSAGE = "message";

    private static final String TAG = CrashlyticsLoggingTree.class.getSimpleName();

    private final FirebaseCrashlytics crashlytics;

    public CrashlyticsLoggingTree() {
        super();
        crashlytics = FirebaseCrashlytics.getInstance();
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        try {
            String priorityTAG;
            switch (priority) {
                default:
                case Log.DEBUG:
                    priorityTAG = "DEBUG";
                    break;
                case Log.INFO:
                    priorityTAG = "INFO";
                    break;
                case Log.VERBOSE:
                    priorityTAG = "VERBOSE";
                    break;
                case Log.WARN:
                    priorityTAG = "WARN";
                    break;
                case Log.ERROR:
                    priorityTAG = "ERROR";
                    break;
            }

            crashlytics.setCustomKey(KEY_PRIORITY, priorityTAG);
            crashlytics.setCustomKey(KEY_TAG, tag);
            crashlytics.setCustomKey(KEY_MESSAGE, message);

            if (tag != null) {
                crashlytics.log(String.format("%s/%s: %s", priorityTAG, tag, message));
            } else {
                crashlytics.log(String.format("%s/%s", priorityTAG, message));

            }

            if (t != null) {
                crashlytics.recordException(t);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while logging : " + e);
        }
    }
}
