package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.common.controls.BaseForecastItemViewModel
import com.thewizrd.simpleweather.controls.WeatherDetailItem

class WeatherDetailsAdapter<T : BaseForecastItemViewModel> :
    PagedListAdapter<T, WeatherDetailsAdapter<T>.ViewHolder> {
    constructor() : super(diffCallback as DiffUtil.ItemCallback<T>)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<BaseForecastItemViewModel>() {
            override fun areItemsTheSame(
                oldItem: BaseForecastItemViewModel,
                newItem: BaseForecastItemViewModel
            ): Boolean {
                return ObjectsCompat.equals(oldItem.date, newItem.date)
            }

            override fun areContentsTheSame(
                oldItem: BaseForecastItemViewModel,
                newItem: BaseForecastItemViewModel
            ): Boolean {
                return ObjectsCompat.equals(oldItem, newItem)
            }
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(private val mDetailPanel: WeatherDetailItem) :
        RecyclerView.ViewHolder(mDetailPanel) {
        fun bind(model: BaseForecastItemViewModel?) {
            mDetailPanel.bind(model)
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(WeatherDetailItem(parent.context))
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.bind(getItem(position))
    }
}