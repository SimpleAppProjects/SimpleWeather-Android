package com.thewizrd.simpleweather.snackbar;

public interface SnackbarManagerInterface {
    void initSnackManager();

    void showSnackbar(Snackbar snackbar, com.google.android.material.snackbar.Snackbar.Callback callback);

    void dismissAllSnackbars();

    void unloadSnackManager();
}
