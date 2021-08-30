package com.thewizrd.simpleweather.snackbar;

import static com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL;
import static com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import java.util.Stack;

/**
 * Manager for the snackbar of an activity or fragment
 * There should be only one SnackbarManager and one snackbar in the activity or fragment.
 * Based on the SnackbarManager implementation for Chromium on Android
 */
public final class SnackbarManager {
    private com.google.android.material.snackbar.Snackbar mSnackbarView;
    private final Stack<SnackbarPair> mSnacks;
    private final Handler mMainHandler;
    private final View mParentView;
    private @BaseTransientBottomBar.AnimationMode
    int mAnimationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE;
    private boolean isSwipeDismissEnabled = false;
    private View mAnchorView;

    public void setSwipeDismissEnabled(boolean swipeDismissEnabled) {
        isSwipeDismissEnabled = swipeDismissEnabled;
    }

    public void setAnchorView(@Nullable View anchorView) {
        mAnchorView = anchorView;
    }

    public void setAnchorView(@IdRes int viewId) {
        mAnchorView = mParentView.findViewById(viewId);
        if (this.mAnchorView == null) {
            throw new IllegalArgumentException("Unable to find anchor view with id: " + viewId);
        }
    }

    public void setAnimationMode(@BaseTransientBottomBar.AnimationMode int animationMode) {
        mAnimationMode = animationMode;
    }

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
    @MainThread
    public void show(@NonNull final Snackbar snackbar, @Nullable com.google.android.material.snackbar.Snackbar.Callback callback) {
        // Add current snackbar to stack
        mSnacks.push(new SnackbarPair(snackbar, callback));

        // Update SnackBar view
        updateView();
    }

    /**
     * Dismisses all snackbars.
     */
    @MainThread
    public void dismissAll() {
        if (mSnacks.empty()) return;

        while (!mSnacks.empty()) {
            // Pop out SnackPair
            SnackbarPair snackbarPair = mSnacks.pop();
            // Perform dismiss action
            if (snackbarPair.callback != null)
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
        if (snackbarPair.callback != null)
            snackbarPair.callback.onDismissed(mSnackbarView, event);
        // Return and let mgr updateview
    }

    /**
     * Update the Snackbar view
     */
    @MainThread
    private void updateView() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            // NOT ON THE MAIN THREAD!!
            throw new IllegalStateException("Cannot update the Snackbar view off the main thread");
        }

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
                        return isSwipeDismissEnabled;
                    }
                });
                snackView.setAnimationMode(mAnimationMode);
                snackView.setAnchorView(mAnchorView);
                TextView snackTextView = snackView.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                if (snackTextView != null) {
                    snackTextView.setMaxLines(Integer.MAX_VALUE);
                }
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
                    if (snackPair.callback != null)
                        snackPair.callback.onDismissed(mSnackbarView, BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION);
                    if (!mSnacks.isEmpty()) mSnacks.pop();
                    updateView();
                }
            });
            if (!mSnackbarView.isShown()) mSnackbarView.show();

            mMainHandler.removeCallbacks(mHideRunnable);
            int durationMs = snackPair.snackbar.getDuration();
            if (durationMs >= 0) {
                durationMs *= App.getInstance().getSettingsManager().getAnimatorScale();
                mMainHandler.postDelayed(mHideRunnable, durationMs);
            }
        }
    }

    private final com.google.android.material.snackbar.Snackbar.Callback callback = new com.google.android.material.snackbar.Snackbar.Callback() {
        @Override
        public void onShown(com.google.android.material.snackbar.Snackbar sb) {
            super.onShown(sb);
            final SnackbarPair snackPair = getCurrentSnackPair();
            if (snackPair != null && snackPair.callback != null)
                snackPair.callback.onShown(sb);
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

    private static class SnackbarPair {
        private final Snackbar snackbar;
        private final com.google.android.material.snackbar.Snackbar.Callback callback;

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