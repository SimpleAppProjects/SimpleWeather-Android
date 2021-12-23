package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding
import java.util.*

class AQIForecastAdapter : ListAdapter<AirQualityViewModel, AQIForecastAdapter.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<AirQualityViewModel>) : super(config)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<AirQualityViewModel>() {
            override fun areItemsTheSame(oldItem: AirQualityViewModel, newItem: AirQualityViewModel): Boolean {
                return Objects.equals(oldItem.date, newItem.date)
            }

            override fun areContentsTheSame(oldItem: AirQualityViewModel, newItem: AirQualityViewModel): Boolean {
                return Objects.equals(oldItem, newItem)
            }
        }
    }

    inner class ViewHolder(private val binding: LayoutAqiForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindModel(aqiData: AirQualityViewModel) {
            binding.viewModel = aqiData
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutAqiForecastItemBinding.inflate(LayoutInflater.from(parent.context)).apply {
            this.root.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindModel(getItem(position))
    }
}