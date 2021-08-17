package com.thewizrd.simpleweather.preferences;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.thewizrd.shared_resources.lifecycle.LifecycleAwarePreferenceFragmentCompat;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface;

public abstract class CustomPreferenceFragmentCompat extends LifecycleAwarePreferenceFragmentCompat implements SnackbarManagerInterface {

    private AppCompatActivity mActivity;
    private SnackbarManager mSnackMgr;
    private final SettingsManager settingsMgr = App.getInstance().getSettingsManager();

    public final AppCompatActivity getAppCompatActivity() {
        return mActivity;
    }

    protected final SettingsManager getSettingsManager() {
        return settingsMgr;
    }

    @Nullable
    public abstract SnackbarManager createSnackManager();

    @CallSuper
    public final void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = createSnackManager();
        }
    }

    public void showSnackbar(@NonNull final com.thewizrd.simpleweather.snackbar.Snackbar snackbar) {
        showSnackbar(snackbar, null);
    }

    @Override
    public void showSnackbar(@NonNull final com.thewizrd.simpleweather.snackbar.Snackbar snackbar, final com.google.android.material.snackbar.Snackbar.Callback callback) {
        runWithView(() -> {
            if (mSnackMgr == null && isAlive()) {
                mSnackMgr = createSnackManager();
            }
            if (mSnackMgr != null && mActivity != null) {
                mSnackMgr.show(snackbar, callback);
            }
        });
    }

    @Override
    public void dismissAllSnackbars() {
        runOnUiThread(() -> {
            if (mSnackMgr != null) mSnackMgr.dismissAll();
        });
    }

    @Override
    public void unloadSnackManager() {
        dismissAllSnackbars();
        mSnackMgr = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            initSnackManager();
        } else {
            dismissAllSnackbars();
        }
    }

    @Override
    public void onPause() {
        unloadSnackManager();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        super.onDestroy();
    }
}
