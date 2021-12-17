package com.thewizrd.simpleweather.helpers

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class AutoFitGridLayoutManager(context: Context, columnWidth: Int) : GridLayoutManager(context, 1) {
    private var mColumnWidth: Int = 0
    private var mColumnWidthChanged = true

    init {
        setColumnWidth(columnWidth)
    }

    fun setColumnWidth(columnWidth: Int) {
        if (columnWidth > 0 && columnWidth != mColumnWidth) {
            mColumnWidth = columnWidth
            mColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (mColumnWidthChanged && mColumnWidth > 0) {
            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = max(1, totalSpace / mColumnWidth)
            setSpanCount(spanCount)
            mColumnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }
}