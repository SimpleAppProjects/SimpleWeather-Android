package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.databinding.ChartsBargraphpanelBinding
import com.thewizrd.simpleweather.databinding.ChartsForecastgraphpanelBinding
import java.util.*

class ChartsItemAdapter : ListAdapter<ForecastGraphViewModel, RecyclerView.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<ForecastGraphViewModel>) : super(config)

    private class ChartType {
        companion object {
            const val LineView = 0
            const val BarChart = 1
        }
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ForecastGraphViewModel>() {
            override fun areItemsTheSame(oldItem: ForecastGraphViewModel, newItem: ForecastGraphViewModel): Boolean {
                return Objects.equals(oldItem, newItem)
            }

            override fun areContentsTheSame(oldItem: ForecastGraphViewModel, newItem: ForecastGraphViewModel): Boolean {
                return Objects.equals(oldItem.graphData, newItem.graphData)
            }
        }
    }

    inner class LineViewViewHolder(private val binding: ChartsForecastgraphpanelBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.forecastGraphPanel.setDrawIconLabels(false)
        }

        fun bind(model: ForecastGraphViewModel) {
            binding.graphModel = model
            binding.executePendingBindings()
        }
    }

    inner class BarChartViewViewHolder(private val binding: ChartsBargraphpanelBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.barGraphPanel.setDrawIconLabels(false)
        }

        fun bind(model: ForecastGraphViewModel) {
            binding.graphData = model.graphData as BarGraphData?
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ChartType.BarChart) {
            BarChartViewViewHolder(ChartsBargraphpanelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            LineViewViewHolder(ChartsForecastgraphpanelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = getItem(position)

        if (holder is LineViewViewHolder) {
            holder.bind(model)
        } else if (holder is BarChartViewViewHolder) {
            holder.bind(model)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).graphData) {
            is BarGraphData -> ChartType.BarChart
            else -> ChartType.LineView
        }
    }
}