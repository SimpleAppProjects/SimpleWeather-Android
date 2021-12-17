package com.thewizrd.simpleweather.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.controls.DetailItemViewModel
import com.thewizrd.simpleweather.controls.DetailCard
import java.util.*

class DetailsItemAdapter : ListAdapter<DetailItemViewModel, DetailsItemAdapter.ViewHolder> {
    constructor() : super(diffCallback)
    constructor(config: AsyncDifferConfig<DetailItemViewModel>) : super(config)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<DetailItemViewModel>() {
            override fun areItemsTheSame(oldItem: DetailItemViewModel, newItem: DetailItemViewModel): Boolean {
                return Objects.equals(oldItem.detailsType, newItem.detailsType)
            }

            override fun areContentsTheSame(oldItem: DetailItemViewModel, newItem: DetailItemViewModel): Boolean {
                return Objects.equals(oldItem, newItem)
            }
        }
    }

    inner class ViewHolder(private val mDetailCard: DetailCard) : RecyclerView.ViewHolder(mDetailCard) {
        fun bind(model: DetailItemViewModel) {
            mDetailCard.bindModel(model)
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DetailCard(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).detailsType.value.toLong()
    }
}