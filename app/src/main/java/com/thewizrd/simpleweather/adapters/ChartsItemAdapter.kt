package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.simpleweather.R
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
                return Objects.equals(oldItem.graphType, newItem.graphType)
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recyclerView.doOnPreDraw {
            if (recyclerView.context.isLargeTablet()) {
                val maxWidth = recyclerView.context.resources.getDimensionPixelSize(R.dimen.wnow_max_view_width)
                if (recyclerView.measuredWidth > maxWidth && recyclerView.itemDecorationCount == 0) {
                    recyclerView.addItemDecoration(SpacerItemDecoration(
                            horizontalSpace = (recyclerView.measuredWidth - maxWidth)
                    ))
                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        holder.itemView.doOnPreDraw {
            setMaxWidthForView(it)
        }
    }

    private fun setMaxWidthForView(view: View) {
        if (view.context.isLargeTablet()) {
            val maxWidth = view.context.resources.getDimensionPixelSize(R.dimen.wnow_max_view_width)
            if (view.measuredWidth > maxWidth) {
                view.updateLayoutParams {
                    width = maxWidth
                }
            } else if (view.visibility != View.VISIBLE) {
                view.doOnNextLayout {
                    setMaxWidthForView(view)
                }
            }
        }
    }
}