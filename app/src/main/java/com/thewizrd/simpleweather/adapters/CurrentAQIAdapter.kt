package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding

class CurrentAQIAdapter : RecyclerView.Adapter<CurrentAQIAdapter.ViewHolder>() {
    private var aqiModel: AirQualityViewModel? = null

    inner class ViewHolder(private val binding: LayoutAqiForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context = itemView.context

        fun bindModel(aqiData: AirQualityViewModel?) {
            binding.viewModel = aqiData
            binding.executePendingBindings()
            binding.aqiDateLabel.text = context.getString(R.string.time_current)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutAqiForecastItemBinding.inflate(LayoutInflater.from(parent.context)).apply {
            this.root.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindModel(aqiModel)
    }

    override fun getItemCount(): Int {
        return if (aqiModel != null) 1 else 0
    }

    fun updateItem(aqiModel: AirQualityViewModel?) {
        val oldItem = this.aqiModel
        this.aqiModel = aqiModel

        if (aqiModel != null) {
            if (oldItem != null) {
                // Replaced
                notifyItemChanged(0)
            } else {
                // Added
                notifyItemInserted(0)
            }
        } else {
            if (oldItem != null) {
                // Removed
                notifyItemRemoved(0)
            } else {
                this.aqiModel = null
                // Nothing changed
            }
        }
    }
}