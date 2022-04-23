package com.thewizrd.simpleweather.adapters

import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.DiffUtil
import com.thewizrd.common.controls.BaseForecastItemViewModel

class ForecastDiffer<T : BaseForecastItemViewModel> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(
        oldItem: T,
        newItem: T
    ): Boolean {
        return ObjectsCompat.equals(oldItem.date, newItem.date)
    }

    override fun areContentsTheSame(
        oldItem: T,
        newItem: T
    ): Boolean {
        return ObjectsCompat.equals(oldItem, newItem)
    }
}