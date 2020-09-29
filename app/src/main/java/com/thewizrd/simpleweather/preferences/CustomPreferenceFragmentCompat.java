package com.thewizrd.simpleweather.preferences;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.thewizrd.shared_resources.lifecycle.LifecycleAwarePreferenceFragmentCompat;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface;

public abstract class CustomPreferenceFragmentCompat extends LifecycleAwarePreferenceFragmentCompat implements SnackbarManagerInterface {

    private AppCompatActivity mActivity;
    private SnackbarManager mSnackMgr;

    public final AppCompatActivity getAppCompatActivity() {
        return mActivity;
    }

    @Nullable
    public abstract SnackbarManager createSnackManager();

    @CallSuper
    public final void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = createSnackManager();
        }
    }

    @Override
    public void showSnackbar(@NonNull final com.thewizrd.simpleweather.snackbar.Snackbar snackbar, final com.google.android.material.snackbar.Snackbar.Callback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr == null && isAlive()) {
                    mSnackMgr = createSnackManager();
                }
                if (mSnackMgr != null && mActivity != null) {
                    mSnackMgr.show(snackbar, callback);
                }
            }
        });
    }

    @Override
    public void dismissAllSnackbars() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.dismissAll();
            }
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
        super.onDetach();
        mActivity = null;
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
        super.onDestroy();
        mActivity = null;
    }
}
