package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding
import java.util.*

class AQIForecastAdapter : ListAdapter<AirQuality, AQIForecastAdapter.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<AirQuality>) : super(config)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<AirQuality>() {
            override fun areItemsTheSame(oldItem: AirQuality, newItem: AirQuality): Boolean {
                return Objects.equals(oldItem.date, newItem.date)
            }

            override fun areContentsTheSame(oldItem: AirQuality, newItem: AirQuality): Boolean {
                return Objects.equals(oldItem, newItem)
            }
        }
    }

    inner class ViewHolder(private val binding: LayoutAqiForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindModel(aqiData: AirQuality) {
            binding.aqiDateLabel.text = aqiData.date?.format(
                    DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.DAY_OF_THE_WEEK)
            )

            binding.airQuality = aqiData
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