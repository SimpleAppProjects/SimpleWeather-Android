package com.thewizrd.simpleweather.snackbar

import android.app.Activity
import com.google.android.material.snackbar.Snackbar as MaterialSnackbar

interface SnackbarManagerInterface {
    fun createSnackManager(activity: Activity): SnackbarManager?
    fun initSnackManager(activity: Activity)
    fun showSnackbar(snackbar: Snackbar, callback: MaterialSnackbar.Callback?)

    fun dismissAllSnackbars()
    fun unloadSnackManager()
}