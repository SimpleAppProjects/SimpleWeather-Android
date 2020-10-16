package com.thewizrd.simpleweather.updates;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallException;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallErrorCode;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Tasks;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.reflect.TypeToken;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class InAppUpdateManager {
    private Context mAppContext;
    private AppUpdateManager appUpdateManager;
    private AppUpdateInfo appUpdateInfo;
    private UpdateInfo configUpdateinfo;

    private InAppUpdateManager(@NonNull Context context) {
        mAppContext = context.getApplicationContext();
        appUpdateManager = AppUpdateManagerFactory.create(mAppContext);
    }

    public static InAppUpdateManager create(@NonNull Context context) {
        return new InAppUpdateManager(context);
    }

    public boolean checkIfUpdateAvailable() {
        try {
            // Returns an intent object that you use to check for an update.
            appUpdateInfo = Tasks.await(appUpdateManager.getAppUpdateInfo());

            // Checks that the platform will allow the specified type of update.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                FeatureSettings.setUpdateAvailable(true);

                // Check priority of update
                List<UpdateInfo> remoteUpdateInfo = getRemoteUpdateInfo();
                configUpdateinfo = Iterables.find(remoteUpdateInfo, new Predicate<UpdateInfo>() {
                    @Override
                    public boolean apply(@NullableDecl UpdateInfo input) {
                        return input != null && input.getVersionCode() == appUpdateInfo.availableVersionCode();
                    }
                }, null);

                if (configUpdateinfo != null) {
                    return true;
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);

            if (e.getCause() instanceof InstallException) {
                int errorCode = ((InstallException) e.getCause()).getErrorCode();
                if (errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED || errorCode == InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND) {
                    Logger.writeLine(Log.INFO, "Checking for update using fallback");
                    return checkIfUpdateAvailableFallback();
                }
            }
        }

        return false;
    }

    private List<UpdateInfo> getRemoteUpdateInfo() throws ExecutionException, InterruptedException {
        // TODO: until this is implemented in Play Console, use Firebase RemoteConfig
        FirebaseRemoteConfig mConfig = FirebaseRemoteConfig.getInstance();
        com.google.android.gms.tasks.Tasks.await(mConfig.fetchAndActivate());
        String json = mConfig.getString("android_updates");
        Type updateTypeToken = new TypeToken<ArrayList<UpdateInfo>>() {
        }.getType();
        List<UpdateInfo> remoteUpdateInfo = JSONParser.deserializer(json, updateTypeToken);
        return remoteUpdateInfo;
    }

    private boolean checkIfUpdateAvailableFallback() {
        try {
            List<UpdateInfo> remoteUpdateInfo = getRemoteUpdateInfo();
            UpdateInfo lastUpdate = Iterables.getLast(remoteUpdateInfo, null);

            if (lastUpdate != null) {
                return Settings.getVersionCode() < lastUpdate.getVersionCode();
            }
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        return false;
    }

    /**
     * Must call {@link InAppUpdateManager#checkIfUpdateAvailable()} before this.
     *
     * @return If update available return priority (1 -> 5, with 5 as high priority); Returns -1 if update not available
     */
    public int getUpdatePriority() {
        if (configUpdateinfo != null) {
            return configUpdateinfo.getUpdatePriority();
        }

        return -1;
    }

    public boolean shouldStartImmediateUpdate() {
        if (appUpdateInfo != null && configUpdateinfo != null) {
            return appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) && configUpdateinfo.getUpdatePriority() > 3;
        }

        return false;
    }

    public Task<Boolean> shouldStartImmediateUpdateFlow() {
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                tcs.setResult(checkIfUpdateAvailable() && shouldStartImmediateUpdate());
            }
        });

        return tcs.getTask();
    }

    public void startImmediateUpdateFlow(@NonNull Activity activity, int requestCode) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    IMMEDIATE,
                    // The current activity making the update request.
                    activity,
                    // Include a request code to later monitor this update request.
                    requestCode);
        } catch (IntentSender.SendIntentException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    public void resumeUpdateIfStarted(@NonNull final Activity activity, final int requestCode) {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                InAppUpdateManager.this.appUpdateInfo = appUpdateInfo;
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an in-app update is already running, resume the update.
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                IMMEDIATE,
                                activity,
                                requestCode);
                    } catch (IntentSender.SendIntentException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && shouldStartImmediateUpdate()) {
                    if (!activity.isDestroyed() && !activity.isFinishing()) {
                        FeatureSettings.setUpdateAvailable(true);
                        startImmediateUpdateFlow(activity, requestCode);
                    }
                }
            }
        });
    }
}
