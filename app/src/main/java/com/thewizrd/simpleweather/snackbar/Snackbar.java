package com.thewizrd.simpleweather.snackbar;

import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.StringRes;

import com.thewizrd.simpleweather.App;

/**
 * Wrapper for the Material Snackbar
 * Snackbar is managed by {@link SnackbarManager}
 */
public class Snackbar {
    private static final int SHORT_DURATION_MS = 2000;
    private static final int LONG_DURATION_MS = 4000;
    private static final int VERYLONG_DURATION_MS = 4000;

    private CharSequence mMessageText;
    private CharSequence mActionText;
    private View.OnClickListener mAction;
    private int mDurationMs;

    private Snackbar() {
    }

    public static Snackbar make(@StringRes int resId, Snackbar.Duration duration) {
        Snackbar snackbar = new Snackbar();
        snackbar.mMessageText = App.getInstance().getAppContext().getText(resId);

        switch (duration) {
            default:
            case SHORT:
                snackbar.mDurationMs = SHORT_DURATION_MS;
                break;
            case LONG:
                snackbar.mDurationMs = LONG_DURATION_MS;
                break;
        }

        return snackbar;
    }

    public static Snackbar make(CharSequence messageText, Snackbar.Duration duration) {
        Snackbar snackbar = new Snackbar();
        snackbar.mMessageText = messageText;

        switch (duration) {
            default:
            case SHORT:
                snackbar.mDurationMs = SHORT_DURATION_MS;
                break;
            case LONG:
                snackbar.mDurationMs = LONG_DURATION_MS;
                break;
            case VERY_LONG:
                snackbar.mDurationMs = VERYLONG_DURATION_MS;
                break;
        }

        return snackbar;
    }

    public void setAction(@StringRes int resId, View.OnClickListener snackBarAction) {
        this.mActionText = App.getInstance().getAppContext().getText(resId);
        this.mAction = snackBarAction;
    }

    public void setAction(CharSequence actionText, View.OnClickListener snackBarAction) {
        this.mActionText = actionText;
        this.mAction = snackBarAction;
    }

    public void setDuration(@IntRange(from = 1) int durationMs) {
        this.mDurationMs = durationMs;
    }

    public CharSequence getMessageText() {
        return mMessageText;
    }

    public CharSequence getActionText() {
        return mActionText;
    }

    public View.OnClickListener getAction() {
        return mAction;
    }

    public int getDuration() {
        return mDurationMs;
    }

    public enum Duration {
        SHORT,
        LONG,
        VERY_LONG
    }
}
