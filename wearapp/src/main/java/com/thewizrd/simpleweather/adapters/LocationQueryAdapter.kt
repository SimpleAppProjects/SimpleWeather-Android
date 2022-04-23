package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.common.adapters.LocationQueryDiffer
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColorStateList
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.WearChipButton

class LocationQueryAdapter :
    ListAdapter<LocationQuery, LocationQueryAdapter.ViewHolder>(LocationQueryDiffer()) {
    private var onClickListener: ListAdapterOnClickInterface<LocationQuery>? = null

    fun setOnClickListener(onClickListener: ListAdapterOnClickInterface<LocationQuery>?) {
        this.onClickListener = onClickListener
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(private val item: WearChipButton) : RecyclerView.ViewHolder(item) {
        fun bind(model: LocationQuery) {
            item.setOnClickListener { v ->
                onClickListener?.onClick(v, model)
            }
            item.setPrimaryText(model.locationName)
            item.setSecondaryText(model.locationCountry)
            item.setIconResource(
                if (model.locationQuery.isNullOrBlank() && model.locationCountry.isNullOrBlank()) {
                    0
                } else {
                    R.drawable.ic_place_white_24dp
                }
            )
            item.findViewById<TextView>(R.id.wear_chip_primary_text)?.apply {
                maxLines = 3
            }
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val item = WearChipButton(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setIconTint(parent.context.getAttrColorStateList(R.attr.colorAccent))
        }
        return ViewHolder(item)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}