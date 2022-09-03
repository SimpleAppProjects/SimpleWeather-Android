package com.thewizrd.simpleweather.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.showInputMethod() {
    val imm =
        this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
    imm.showSoftInput(this, InputMethodManager.SHOW_FORCED)
}

fun View.hideInputMethod() {
    val imm =
        this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}