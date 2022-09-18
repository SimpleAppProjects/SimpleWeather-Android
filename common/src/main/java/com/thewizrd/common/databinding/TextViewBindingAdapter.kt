package com.thewizrd.common.databinding

import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.databinding.BindingAdapter
import com.thewizrd.common.utils.isTextTruncated

object TextViewBindingAdapter {
    @JvmStatic
    @BindingAdapter("clickable")
    fun checkClickable(view: TextView, text: CharSequence?) {
        view.doOnPreDraw {
            it.isClickable = view.isTextTruncated()
        }
    }
}