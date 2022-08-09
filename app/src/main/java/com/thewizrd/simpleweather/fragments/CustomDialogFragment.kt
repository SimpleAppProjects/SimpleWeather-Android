package com.thewizrd.simpleweather.fragments

import android.app.Activity
import androidx.annotation.CallSuper
import com.thewizrd.shared_resources.lifecycle.LifecycleAwareDialogFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.snackbar.SnackbarManagerInterface
import com.google.android.material.snackbar.Snackbar as MaterialSnackbar

abstract class CustomDialogFragment : LifecycleAwareDialogFragment(), SnackbarManagerInterface {
    private var mSnackMgr: SnackbarManager? = null

    abstract override fun createSnackManager(activity: Activity): SnackbarManager?

    @CallSuper
    override fun initSnackManager(activity: Activity) {
        mSnackMgr = createSnackManager(activity)
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
                if (isVisible) {
                    if (mSnackMgr == null) {
                        mSnackMgr = createSnackManager(it)
                    }
                    // Snackbar may check higher up in the view hierarchy
                    // Check if fragment is attached
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