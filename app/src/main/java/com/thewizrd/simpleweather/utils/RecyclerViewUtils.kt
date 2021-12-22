package com.thewizrd.simpleweather.utils

import androidx.recyclerview.widget.RecyclerView

object RecyclerViewUtils {
    @JvmStatic
    fun RecyclerView.containsItemDecoration(itemDecoration: RecyclerView.ItemDecoration): Boolean {
        val itemDecoCount = this.itemDecorationCount

        for (i in 0 until itemDecoCount) {
            val item = this.getItemDecorationAt(i)

            if (item === itemDecoration) {
                return true
            }
        }

        return false
    }
}