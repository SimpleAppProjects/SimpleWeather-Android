package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.DetailItemPanelBinding

class DetailItemAdapter : ListAdapter<DetailItemViewModel, RecyclerView.ViewHolder> {
    constructor() : super(object : DiffUtil.ItemCallback<DetailItemViewModel>() {
        override fun areItemsTheSame(oldItem: DetailItemViewModel, newItem: DetailItemViewModel): Boolean {
            return oldItem.detailsType == newItem.detailsType
        }

        override fun areContentsTheSame(oldItem: DetailItemViewModel, newItem: DetailItemViewModel): Boolean {
            return ObjectsCompat.equals(oldItem, newItem)
        }
    })

    constructor(config: AsyncDifferConfig<DetailItemViewModel?>) : super(config)

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    internal class ViewHolder(private val binding: DetailItemPanelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DetailItemViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.locations_header, parent, false))
            }
            else -> {
                // create a new view
                val inflater = LayoutInflater.from(parent.context)
                val binding = DetailItemPanelBinding.inflate(inflater)
                // set the view's size, margins, paddings and layout parameters
                binding.root.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                ViewHolder(binding)
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderSetterInterface) {
            (holder as HeaderSetterInterface).setHeader()
        } else {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val vh = holder as ViewHolder
            vh.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) 1 else 0
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return if (dataCount == 0) 0 else dataCount + 1
    }

    val dataCount: Int
        get() = super.getItemCount()

    private interface HeaderSetterInterface {
        fun setHeader()
    }

    private class FooterViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), HeaderSetterInterface {
        val header: TextView = itemView.findViewById(R.id.header)

        init {
            header.setTextColor(Colors.WHITE)
        }

        override fun setHeader() {
            val context = header.context
            val creditPrefix = context.getString(R.string.credit_prefix)
            val weatherAPI = WeatherManager.getInstance().weatherAPI

            val entry = WeatherAPI.APIs.find { wapi: ProviderEntry? -> wapi != null && weatherAPI == wapi.value }
            val credit = String.format("%s %s",
                    creditPrefix,
                    entry?.toString() ?: WeatherIcons.EM_DASH)

            header.text = credit
        }
    }
}