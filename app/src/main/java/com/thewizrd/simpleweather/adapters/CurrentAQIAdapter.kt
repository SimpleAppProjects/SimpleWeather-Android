package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.AirQualityViewModel
import com.thewizrd.shared_resources.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding
import com.thewizrd.simpleweather.utils.RecyclerViewUtils.containsItemDecoration

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