package com.thewizrd.common.utils

import android.widget.TextView

fun TextView.isTextTruncated(): Boolean {
    val layout = this.layout

    if (layout != null) {
        val lines = layout.lineCount
        if (lines > 0) {
            val ellipsisCount = layout.getEllipsisCount(lines - 1)
            return ellipsisCount > 0
        }
    }

    return false
}