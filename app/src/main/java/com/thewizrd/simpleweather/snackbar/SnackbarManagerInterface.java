package com.thewizrd.simpleweather.snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface SnackbarManagerInterface {
    @Nullable
    SnackbarManager createSnackManager();

    void initSnackManager();

    void showSnackbar(@NonNull Snackbar snackbar, @Nullable com.google.android.material.snackbar.Snackbar.Callback callback);

    void dismissAllSnackbars();

    void unloadSnackManager();
}
