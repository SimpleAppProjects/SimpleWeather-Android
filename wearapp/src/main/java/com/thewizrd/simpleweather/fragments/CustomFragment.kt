package com.thewizrd.simpleweather.fragments

import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import com.thewizrd.shared_resources.lifecycle.LifecycleAwareFragment

abstract class CustomFragment : LifecycleAwareFragment() {
    @IntDef(Toast.LENGTH_SHORT, Toast.LENGTH_LONG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ToastDuration

    fun showToast(@StringRes resId: Int, @ToastDuration duration: Int) {
        runWithView {
            context?.let {
                Toast.makeText(it, resId, duration).show()
            }
        }
    }

    fun showToast(message: CharSequence?, @ToastDuration duration: Int) {
        runWithView {
            context?.let {
                if (isVisible) {
                    Toast.makeText(it, message, duration).show()
                }
            }
        }
    }
}