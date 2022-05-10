package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.databinding.ChartsBargraphpanelBinding
import java.util.*

class AQIForecastGraphAdapter : ListAdapter<BarGraphData, AQIForecastGraphAdapter.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<BarGraphData>) : super(config)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<BarGraphData>() {
            override fun areItemsTheSame(oldItem: BarGraphData, newItem: BarGraphData): Boolean {
                return Objects.equals(oldItem.graphLabel, newItem.graphLabel)
            }

            override fun areContentsTheSame(oldItem: BarGraphData, newItem: BarGraphData): Boolean {
                return Objects.equals(oldItem, newItem)
            }
        }
    }

    inner class ViewHolder(internal val binding: ChartsBargraphpanelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.barGraphPanel.setScrollingEnabled(false)
            binding.barGraphPanel.setDrawIconLabels(false)
            binding.barGraphPanel.setFillParentWidth(!itemView.context.isLargeTablet())
            binding.barGraphPanel.setGraphMaxWidth(
                if (itemView.context.isLargeTablet()) {
                    -1
                } else {
                    itemView.context.resources.getDimensionPixelSize(R.dimen.graph_max_width)
                }
            )
        }

        fun bindModel(aqiData: BarGraphData) {
            binding.graphData = aqiData
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ChartsBargraphpanelBinding.inflate(LayoutInflater.from(parent.context)).apply {
                this.root.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindModel(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.binding.barGraphPanel.requestGraphLayout()
    }
}