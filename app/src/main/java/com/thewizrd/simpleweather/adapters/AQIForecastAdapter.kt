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
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.shared_resources.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding
import com.thewizrd.simpleweather.utils.RecyclerViewUtils.containsItemDecoration
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

    // TODO: maybe move this to fragment instead?
    private var mItemDecoration: RecyclerView.ItemDecoration? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        recyclerView.doOnPreDraw {
            if (recyclerView.context.isLargeTablet()) {
                val maxWidth = recyclerView.context.resources.getDimensionPixelSize(R.dimen.wnow_max_view_width)
                if (recyclerView.measuredWidth > maxWidth) {
                    if (mItemDecoration == null || !recyclerView.containsItemDecoration(mItemDecoration!!)) {
                        recyclerView.addItemDecoration(SpacerItemDecoration(
                                horizontalSpace = (recyclerView.measuredWidth - maxWidth)
                        ).also {
                            mItemDecoration = it
                        })
                    }
                }
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        mItemDecoration?.let {
            recyclerView.removeItemDecoration(it)
        }
        mItemDecoration = null
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