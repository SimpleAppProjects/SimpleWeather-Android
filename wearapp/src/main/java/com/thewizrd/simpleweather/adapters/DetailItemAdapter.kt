package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.ObjectsCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.simpleweather.databinding.DetailItemPanelBinding

class DetailItemAdapter : ListAdapter<DetailItemViewModel, DetailItemAdapter.ViewHolder> {
    constructor() : super(object : DiffUtil.ItemCallback<DetailItemViewModel>() {
        override fun areItemsTheSame(
            oldItem: DetailItemViewModel,
            newItem: DetailItemViewModel
        ): Boolean {
            return oldItem.detailsType == newItem.detailsType
        }

        override fun areContentsTheSame(
            oldItem: DetailItemViewModel,
            newItem: DetailItemViewModel
        ): Boolean {
            return ObjectsCompat.equals(oldItem, newItem)
        }
    })

    constructor(config: AsyncDifferConfig<DetailItemViewModel?>) : super(config)

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(private val binding: DetailItemPanelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: DetailItemViewModel) {
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DetailItemPanelBinding.inflate(inflater)
        binding.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}