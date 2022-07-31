package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.simpleweather.databinding.WeatherMinforecastPanelBinding
import com.thewizrd.simpleweather.viewmodels.MinutelyForecastViewModel

class MinutelyItemAdapter : ListAdapter<MinutelyForecastViewModel, RecyclerView.ViewHolder> {
    constructor() : super(object : DiffUtil.ItemCallback<MinutelyForecastViewModel>() {
        override fun areItemsTheSame(
            oldItem: MinutelyForecastViewModel,
            newItem: MinutelyForecastViewModel
        ): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(
            oldItem: MinutelyForecastViewModel,
            newItem: MinutelyForecastViewModel
        ): Boolean {
            return ObjectsCompat.equals(oldItem, newItem)
        }
    })

    constructor(config: AsyncDifferConfig<MinutelyForecastViewModel?>) : super(config)

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    internal class ViewHolder(private val binding: WeatherMinforecastPanelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: MinutelyForecastViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // create a new view
        val inflater = LayoutInflater.from(parent.context)
        val binding = WeatherMinforecastPanelBinding.inflate(inflater)
        // set the view's size, margins, paddings and layout parameters
        binding.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val vh = holder as ViewHolder
        vh.bind(getItem(position))
    }
}