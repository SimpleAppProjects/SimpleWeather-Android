package com.thewizrd.shared_resources.adapters

import androidx.recyclerview.widget.DiffUtil
import com.thewizrd.shared_resources.controls.LocationQueryViewModel

class LocationQueryDiffer : DiffUtil.ItemCallback<LocationQueryViewModel>() {
    override fun areItemsTheSame(
        oldItem: LocationQueryViewModel,
        newItem: LocationQueryViewModel
    ): Boolean {
        return oldItem.locationQuery == newItem.locationQuery
    }

    override fun areContentsTheSame(
        oldItem: LocationQueryViewModel,
        newItem: LocationQueryViewModel
    ): Boolean {
        return oldItem == newItem
    }
}