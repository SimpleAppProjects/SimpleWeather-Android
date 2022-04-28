package com.thewizrd.simpleweather.preferences

import android.app.Activity
import androidx.annotation.CallSuper
import com.thewizrd.shared_resources.lifecycle.LifecycleAwarePreferenceFragmentCompat
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface
import com.google.android.material.snackbar.Snackbar as MaterialSnackbar

abstract class CustomPreferenceFragmentCompat : LifecycleAwarePreferenceFragmentCompat(),
    SnackbarManagerInterface {
    private var mSnackMgr: SnackbarManager? = null

    abstract override fun createSnackManager(activity: Activity): SnackbarManager?

    @CallSuper
    override fun initSnackManager(activity: Activity) {
        if (mSnackMgr == null) {
            mSnackMgr = createSnackManager(activity)
        }
    }

    fun showSnackbar(snackbar: Snackbar) {
        showSnackbar(snackbar, null)
    }

    override fun showSnackbar(
        snackbar: Snackbar,
        callback: MaterialSnackbar.Callback?
    ) {
        runWithView {
            activity?.let {
                if (isAlive) {
                    if (mSnackMgr == null) {
                        mSnackMgr = createSnackManager(it)
                    }
                    mSnackMgr?.show(snackbar, callback)
                }
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

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            initSnackManager(requireActivity())
        } else {
            dismissAllSnackbars()
        }
    }

    override fun onPause() {
        unloadSnackManager()
        super.onPause()
    }
}