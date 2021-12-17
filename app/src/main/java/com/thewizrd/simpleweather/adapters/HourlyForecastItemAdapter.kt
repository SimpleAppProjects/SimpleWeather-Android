package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.simpleweather.controls.viewmodels.HourlyForecastNowViewModel
import com.thewizrd.simpleweather.databinding.HourlyForecastItemBinding

class HourlyForecastItemAdapter : ListAdapter<HourlyForecastNowViewModel, HourlyForecastItemAdapter.ViewHolder> {
    constructor(diffCallback: DiffUtil.ItemCallback<HourlyForecastNowViewModel>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<HourlyForecastNowViewModel>) : super(config)

    var onClickListener: RecyclerOnClickListenerInterface? = null

    inner class ViewHolder(private val binding: HourlyForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: HourlyForecastNowViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(HourlyForecastItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener {
            onClickListener?.onClick(it, position)
        }
    }
}