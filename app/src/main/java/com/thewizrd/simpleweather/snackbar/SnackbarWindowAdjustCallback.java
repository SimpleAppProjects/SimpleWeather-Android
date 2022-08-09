package com.thewizrd.simpleweather.snackbar;

import android.app.Activity;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarWindowAdjustCallback extends Snackbar.Callback {
    private final Activity mActivity;
    private final int softInputMode;

    public SnackbarWindowAdjustCallback(@NonNull Activity parentActivity) {
        mActivity = parentActivity;
        softInputMode = mActivity.getWindow().getAttributes().softInputMode;
    }

    @Override
    public void onShown(Snackbar sb) {
        super.onShown(sb);
        if (mActivity != null)
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onDismissed(Snackbar transientBottomBar, int event) {
        super.onDismissed(transientBottomBar, event);
        if (mActivity != null)
            mActivity.getWindow().setSoftInputMode(softInputMode);
    }
}
