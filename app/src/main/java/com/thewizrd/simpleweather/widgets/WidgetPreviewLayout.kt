package com.thewizrd.simpleweather.widgets

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * WidgetPreviewLayout
 *
 * FrameLayout for displaying widget layout inflated from RemoteViews
 * This layout intercepts all touch events to avoid invoking onClick events created by RemoteViews
 */
class WidgetPreviewLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        isClickable = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isContextClickable = false
        }
        isLongClickable = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }
}