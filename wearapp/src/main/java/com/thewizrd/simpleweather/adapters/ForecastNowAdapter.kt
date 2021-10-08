package com.thewizrd.simpleweather.adapters

import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.scale
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel
import com.thewizrd.shared_resources.controls.ForecastItemViewModel
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.simpleweather.databinding.ForecastItemBinding
import com.thewizrd.simpleweather.databinding.HrforecastItemBinding

class ForecastNowAdapter<T : BaseForecastItemViewModel> :
    ListAdapter<T, RecyclerView.ViewHolder>(ForecastDiffer()) {
    private object ItemType {
        const val FORECAST = 0
        const val HOURLYFORECAST = 1
    }

    private var onClickListener: RecyclerOnClickListenerInterface? = null

    fun setOnClickListener(listener: RecyclerOnClickListenerInterface?) {
        onClickListener = listener
    }

    inner class ForecastViewHolder(val binding: ForecastItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ForecastItemViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    inner class HourlyForecastViewHolder(val binding: HrforecastItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: HourlyForecastItemViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()

            val fcast = model.forecast
            val is24hr = DateFormat.is24HourFormat(itemView.context)
            val time: String
            val timeSuffix: String
            if (is24hr) {
                time = fcast.date.format(
                    DateTimeUtils.ofPatternForUserLocale(
                        DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)
                    )
                )
                timeSuffix = ""
            } else {
                time = fcast.date.format(DateTimeUtils.ofPatternForUserLocale("h"))
                timeSuffix = fcast.date.format(DateTimeUtils.ofPatternForUserLocale("a"))
            }

            val sb = SpannableStringBuilder(time)
            sb.scale(0.8f) {
                this.append(timeSuffix)
            }

            binding.hrforecastDate.text = sb
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is HourlyForecastItemViewModel) {
            ItemType.HOURLYFORECAST
        } else {
            ItemType.FORECAST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == ItemType.HOURLYFORECAST) {
            val binding = HrforecastItemBinding.inflate(inflater)
            binding.root.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            HourlyForecastViewHolder(binding)
        } else {
            val binding = ForecastItemBinding.inflate(inflater)
            binding.root.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            ForecastViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ForecastNowAdapter<*>.HourlyForecastViewHolder) {
            holder.bind(getItem(position) as HourlyForecastItemViewModel)
        } else if (holder is ForecastNowAdapter<*>.ForecastViewHolder) {
            holder.bind(getItem(position) as ForecastItemViewModel)
        }

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(it, position)
        }
    }
}