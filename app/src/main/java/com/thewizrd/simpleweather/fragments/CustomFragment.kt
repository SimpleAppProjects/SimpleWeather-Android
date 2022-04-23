package com.thewizrd.simpleweather.fragments

import android.content.Context
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.lifecycle.LifecycleAwareFragment
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface

abstract class CustomFragment : LifecycleAwareFragment(), SnackbarManagerInterface {
    var appCompatActivity: AppCompatActivity? = null
        private set
    private var mSnackMgr: SnackbarManager? = null

    protected fun getSettingsManager(): SettingsManager {
        return appLib.settingsManager
    }

    abstract override fun createSnackManager(): SnackbarManager

    @CallSuper
    override fun initSnackManager() {
        mSnackMgr = createSnackManager()
    }

    fun showSnackbar(snackbar: Snackbar) {
        showSnackbar(snackbar, null)
    }

    override fun showSnackbar(
        snackbar: Snackbar,
        callback: com.google.android.material.snackbar.Snackbar.Callback?
    ) {
        runWithView {
            if (appCompatActivity != null && isVisible) {
                if (mSnackMgr == null) {
                    mSnackMgr = createSnackManager()
                }
                // Snackbar may check higher up in the view hierarchy
                // Check if fragment is attached
                mSnackMgr?.show(snackbar, callback)
            }
        }
    }

    override fun dismissAllSnackbars() {
        runOnUiThread { mSnackMgr?.dismissAll() }
    }

    override fun unloadSnackManager() {
        dismissAllSnackbars()
        mSnackMgr = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appCompatActivity = context as AppCompatActivity
    }

    override fun onDetach() {
        appCompatActivity = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            initSnackManager()
        } else {
            dismissAllSnackbars()
        }
    }

    override fun onPause() {
        unloadSnackManager()
        super.onPause()
    }

    override fun onDestroy() {
        appCompatActivity = null
        super.onDestroy()
    }
}