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
                return Objects.equals(oldItem.graphData, newItem.graphData)
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

    override fun onViewAttachedToWindow(holder: ViewHolder) {
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