package com.thewizrd.common.databinding

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.thewizrd.shared_resources.locationdata.LocationQuery

object LocationQueryBindingAdapter {
    @BindingAdapter("locationPin")
    @JvmStatic
    fun setLocationPinVisibility(view: ImageView, query: LocationQuery) {
        view.isVisible =
            !query.locationQuery.isNullOrBlank() || !query.locationCountry.isNullOrBlank()
    }

    @BindingAdapter("locationRegion")
    @JvmStatic
    fun setLocationRegion(view: TextView, query: LocationQuery) {
        val text = if (query.locationRegion.isNullOrBlank()) {
            query.locationCountry
        } else {
            "${query.locationRegion}, ${query.locationCountry}"
        }

        view.text = text
        view.isVisible = !text.isNullOrBlank()
    }
}