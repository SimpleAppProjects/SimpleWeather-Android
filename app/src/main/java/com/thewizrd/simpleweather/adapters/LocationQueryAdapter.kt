package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.adapters.LocationQueryDiffer
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.databinding.LocationQueryViewBinding
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface

class LocationQueryAdapter :
    ListAdapter<LocationQueryViewModel, LocationQueryAdapter.ViewHolder>(LocationQueryDiffer()) {
    private var onClickListener: ListAdapterOnClickInterface<LocationQueryViewModel>? = null

    fun setOnClickListener(onClickListener: ListAdapterOnClickInterface<LocationQueryViewModel>?) {
        this.onClickListener = onClickListener
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(private val binding: LocationQueryViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: LocationQueryViewModel) {
            binding.root.setOnClickListener { v ->
                onClickListener?.onClick(v, model)
            }
            binding.viewModel = model
            binding.executePendingBindings()
        }
    }

    @SuppressLint("NewApi")  // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LocationQueryViewBinding.inflate(inflater).apply {
            root.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}