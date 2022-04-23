package com.thewizrd.common.adapters

import androidx.recyclerview.widget.DiffUtil
import com.thewizrd.shared_resources.locationdata.LocationQuery

class LocationQueryDiffer : DiffUtil.ItemCallback<LocationQuery>() {
    override fun areItemsTheSame(
        oldItem: LocationQuery,
        newItem: LocationQuery
    ): Boolean {
        return oldItem.locationQuery == newItem.locationQuery
    }

    override fun areContentsTheSame(
        oldItem: LocationQuery,
        newItem: LocationQuery
    ): Boolean {
        return oldItem == newItem
    }
}