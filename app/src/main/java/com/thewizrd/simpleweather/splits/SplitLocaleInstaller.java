package com.thewizrd.simpleweather.splits;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.SplitInstallDialogBinding;

import java.util.Locale;
import java.util.Set;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class SplitLocaleInstaller implements InstallRequest {
    public static final int CONFIRMATION_REQUEST_CODE = 5;

    private final Activity activityContext;
    private final String langCode;

    private final SplitInstallManager splitInstallManager;
    private SplitInstallDialogBinding dialogBinding;
    private AlertDialog alertDialog;

    private SplitLocaleInstaller(@NonNull Activity activity, final String langCode) {
        this.activityContext = activity;
        this.langCode = langCode;
        this.splitInstallManager = SplitInstallManagerFactory.create(activityContext);
    }

    public static InstallRequest installLocale(@NonNull Activity activity, final String langCode) {
        SplitLocaleInstaller installer = new SplitLocaleInstaller(activity, langCode);
        installer.startInstallRequest();
        return installer;
    }

    public void cancelRequest() {
        splitInstallManager.unregisterListener(statelistener);
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
            dialogBinding = null;
        }
    }

    private void startInstallRequest() {
        if (!StringUtils.isNullOrWhitespace(langCode)) {
            try {
                final Set<String> installedLangs = splitInstallManager.getInstalledLanguages();

                if (installedLangs != null && !installedLangs.contains(langCode)) {
                    // Create dialog
                    dialogBinding = SplitInstallDialogBinding.inflate(LayoutInflater.from(activityContext));
                    alertDialog = new AlertDialog.Builder(activityContext)
                            .setView(dialogBinding.getRoot())
                            .setCancelable(true)
                            .setOnCancelListener(dialog -> cancelRequest())
                            .show();

                    // Load and install the requested language.
                    SplitInstallRequest request = SplitInstallRequest.newBuilder()
                            .addLanguage(new Locale(langCode))
                            .build();
                    splitInstallManager.registerListener(statelistener);
                    splitInstallManager.startInstall(request).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            if (activityContext != null) {
                                activityContext.runOnUiThread(this::showRequestFailure);
                            }
                        }
                    });

                    // Update UI to show starting install status...
                    dialogBinding.progress.setIndeterminate(true);
                    dialogBinding.message.setText(R.string.splitlang_start_install);

                    return;
                }
            } catch (Exception e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        updateLocale(activityContext, langCode);
    }

    private void updateLocale(@NonNull Activity activity, final String langCode) {
        LocaleUtils.setLocaleCode(langCode);
        ActivityCompat.recreate(activity);
    }

    final SplitInstallStateUpdatedListener statelistener = new SplitInstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(@NonNull SplitInstallSessionState state) {
            switch (state.status()) {
                case SplitInstallSessionStatus.DOWNLOADING:
                    updateLoadingState(state, R.string.splitlang_downloading);
                    break;
                case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                    /*
                     * This may occur when attempting to download a sufficiently large module.
                     * In order to see this, the application has to be uploaded to the Play Store.
                     * Then features can be requested until the confirmation path is triggered.
                     */
                    try {
                        splitInstallManager.startConfirmationDialogForResult(state, activityContext, CONFIRMATION_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                    break;
                case SplitInstallSessionStatus.INSTALLED:
                    splitInstallManager.unregisterListener(this);
                    updateLocale(activityContext, langCode);
                    break;
                case SplitInstallSessionStatus.INSTALLING:
                    updateLoadingState(state, R.string.splitlang_installing);
                    break;
                case SplitInstallSessionStatus.FAILED:
                    showRequestFailure();
                    break;
                case SplitInstallSessionStatus.PENDING:
                case SplitInstallSessionStatus.UNKNOWN:
                    dialogBinding.progress.setIndeterminate(true);
                    break;
                case SplitInstallSessionStatus.CANCELED:
                case SplitInstallSessionStatus.CANCELING:
                case SplitInstallSessionStatus.DOWNLOADED:
                    break;
            }
        }
    };

    private void updateLoadingState(SplitInstallSessionState state, String message) {
        if (dialogBinding != null) {
            dialogBinding.progress.setIndeterminate(false);
            dialogBinding.progress.setMax((int) state.totalBytesToDownload());
            dialogBinding.progress.setProgress((int) state.bytesDownloaded());
            dialogBinding.message.setText(message);
        }
    }

    private void updateLoadingState(SplitInstallSessionState state, int messageResId) {
        if (dialogBinding != null) {
            updateLoadingState(state, dialogBinding.message.getContext().getString(messageResId));
        }
    }

    private void showRequestFailure() {
        cancelRequest();
        Toast.makeText(activityContext, R.string.splitlang_failure, Toast.LENGTH_SHORT).show();
    }
}
