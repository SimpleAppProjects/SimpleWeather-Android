package com.thewizrd.simpleweather.snackbar;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;

import java.util.Stack;

import static com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL;
import static com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT;

/**
 * Manager for the snackbar of an activity or fragment
 * There should be only one SnackbarManager and one snackbar in the activity or fragment.
 * Based on the SnackbarManager implementation for Chromium on Android
 */
public final class SnackbarManager {
    private com.google.android.material.snackbar.Snackbar mSnackbarView;
    private Stack<SnackbarPair> mSnacks;
    private Handler mMainHandler;
    private View mParentView;

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mSnacks.isEmpty()) {
                removeSnack(DISMISS_EVENT_TIMEOUT);
            }

            updateView();
        }
    };

    /**
     * Constructs a SnackbarManager to show snackbars in the given window.
     *
     * @param parent The ViewGroup used to display snackbars.
     */
    public SnackbarManager(@NonNull View parent) {
        mParentView = parent;
        mMainHandler = new Handler(Looper.getMainLooper());
        mSnacks = new Stack<>();
    }

    /**
     * Shows a snackbar the snackbar
     *
     * @param snackbar The snackbar to show
     * @param callback Optional: the callback which is called when the snackbar is
     *                 dismissed and/or shown
     */
    public void show(@NonNull final Snackbar snackbar, @Nullable com.google.android.material.snackbar.Snackbar.Callback callback) {
        // Add current snackbar to stack
        mSnacks.push(new SnackbarPair(snackbar, callback));

        // Update SnackBar view
        updateView();
    }

    /**
     * Dismisses all snackbars.
     */
    public void dismissAll() {
        if (mSnacks.empty()) return;

        while (!mSnacks.empty()) {
            // Pop out SnackPair
            SnackbarPair snackbarPair = mSnacks.pop();
            // Perform dismiss action
            snackbarPair.callback.onDismissed(mSnackbarView, DISMISS_EVENT_MANUAL);
        }

        mSnacks.clear();
        updateView();
    }

    /**
     * Removes the current snackbar after it times out
     */
    private void removeSnack(@BaseTransientBottomBar.BaseCallback.DismissEvent int event) {
        if (mSnacks.empty()) return;
        // Pop out SnackPair
        SnackbarPair snackbarPair = mSnacks.pop();
        // Perform dismiss action
        snackbarPair.callback.onDismissed(mSnackbarView, event);
        // Return and let mgr updateview
    }

    /**
     * Update the Snackbar view
     */
    private void updateView() {
        // Get current SnackBar
        final SnackbarPair snackPair = getCurrentSnackPair();
        if (snackPair == null) {
            // Remove callbacks
            mMainHandler.removeCallbacks(mHideRunnable);
            // Dismiss view if there are no more snackbars
            if (mSnackbarView != null) {
                mSnackbarView.dismiss();
                mSnackbarView = null;
            }
        } else {
            // Check if SnackBar view instance exists
            if (mSnackbarView == null) {
                com.google.android.material.snackbar.Snackbar snackView =
                        com.google.android.material.snackbar.Snackbar.make(mParentView, "", BaseTransientBottomBar.LENGTH_INDEFINITE);
                snackView.setCallback(callback);
                snackView.setBehavior(new com.google.android.material.snackbar.Snackbar.Behavior() {
                    @Override
                    public boolean canSwipeDismissView(View child) {
                        return false;
                    }
                });
                mSnackbarView = snackView;
            }

            // Update view
            mSnackbarView.setText(snackPair.snackbar.getMessageText());
            mSnackbarView.setAction(snackPair.snackbar.getActionText(), snackPair.snackbar.getAction());
            // Override Snackbar action click listener to prevent it from being dismissed on click
            mSnackbarView.getView().findViewById(R.id.snackbar_action).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (snackPair.snackbar.getAction() != null) {
                        snackPair.snackbar.getAction().onClick(v);
                    }
                    // Now dismiss the Snackbar
                    snackPair.callback.onDismissed(mSnackbarView, BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION);
                    if (!mSnacks.isEmpty()) mSnacks.pop();
                    updateView();
                }
            });
            if (!mSnackbarView.isShown()) mSnackbarView.show();

            mMainHandler.removeCallbacks(mHideRunnable);
            int durationMs = snackPair.snackbar.getDuration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                durationMs *= Settings.getAnimatorScale();
            }
            mMainHandler.postDelayed(mHideRunnable, durationMs);
        }
    }

    private com.google.android.material.snackbar.Snackbar.Callback callback = new com.google.android.material.snackbar.Snackbar.Callback() {
        @Override
        public void onShown(com.google.android.material.snackbar.Snackbar sb) {
            super.onShown(sb);
        }

        @Override
        public void onDismissed(com.google.android.material.snackbar.Snackbar transientBottomBar, int event) {
            super.onDismissed(transientBottomBar, event);

            if (event == DISMISS_EVENT_CONSECUTIVE || event == DISMISS_EVENT_SWIPE) {
                removeSnack(event);
            }
        }
    };

    private SnackbarPair getCurrentSnackPair() {
        if (mSnacks.empty()) return null;
        return mSnacks.peek();
    }

    private class SnackbarPair {
        private Snackbar snackbar;
        private com.google.android.material.snackbar.Snackbar.Callback callback;

        public SnackbarPair(@NonNull Snackbar snackbar, @Nullable com.google.android.material.snackbar.Snackbar.Callback callback) {
            this.snackbar = snackbar;
            this.callback = callback;
        }

        public Snackbar getSnackbar() {
            return snackbar;
        }

        public com.google.android.material.snackbar.Snackbar.Callback getCallback() {
            return callback;
        }
    }
}