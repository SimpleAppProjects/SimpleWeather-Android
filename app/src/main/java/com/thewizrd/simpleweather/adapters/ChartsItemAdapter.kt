package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.databinding.ChartsForecastgraphpanelBinding
import java.util.*

class ChartsItemAdapter : ListAdapter<ForecastGraphViewModel, ChartsItemAdapter.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<ForecastGraphViewModel>) : super(config)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ForecastGraphViewModel>() {
            override fun areItemsTheSame(oldItem: ForecastGraphViewModel, newItem: ForecastGraphViewModel): Boolean {
                return Objects.equals(oldItem.graphType, newItem.graphType)
            }

            override fun areContentsTheSame(oldItem: ForecastGraphViewModel, newItem: ForecastGraphViewModel): Boolean {
                return Objects.equals(oldItem.labelData, newItem.labelData) && Objects.equals(oldItem.seriesData, newItem.seriesData)
            }
        }
    }

    inner class ViewHolder(private val binding: ChartsForecastgraphpanelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ForecastGraphViewModel) {
            binding.graphModel = model
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ChartsForecastgraphpanelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}