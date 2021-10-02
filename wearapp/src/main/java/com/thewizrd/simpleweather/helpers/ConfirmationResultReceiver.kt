package com.thewizrd.simpleweather.helpers

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.wear.widget.ConfirmationOverlay

fun Activity.showConfirmationOverlay(success: Boolean) {
    val overlay = ConfirmationOverlay()
    if (!success) {
        overlay.setType(ConfirmationOverlay.FAILURE_ANIMATION)
    } else {
        overlay.setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
    }
    overlay.showOn(this)
}

fun Fragment.showConfirmationOverlay(success: Boolean) {
    val overlay = ConfirmationOverlay()
    if (!success) {
        overlay.setType(ConfirmationOverlay.FAILURE_ANIMATION)
    } else {
        overlay.setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
    }
    overlay.showAbove(view!!)
}

fun android.app.Fragment.showConfirmationOverlay(success: Boolean) {
    val overlay = ConfirmationOverlay()
    if (!success) {
        overlay.setType(ConfirmationOverlay.FAILURE_ANIMATION)
    } else {
        overlay.setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
    }
    overlay.showAbove(view!!)
}