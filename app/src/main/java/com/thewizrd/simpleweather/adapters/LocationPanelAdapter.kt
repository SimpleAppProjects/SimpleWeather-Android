package com.thewizrd.simpleweather.adapters

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.controls.LocationPanel
import com.thewizrd.simpleweather.controls.LocationPanelUiModel

internal object LocationPanelPayload {
    const val IMAGE_UPDATE = 0
}

internal object LocationPanelItemType {
    @JvmField
    val GPS_PANEL: Int = LocationType.GPS.value
    @JvmField
    val SEARCH_PANEL: Int = LocationType.SEARCH.value
    const val HEADER_GPS = -2
    const val HEADER_FAV = -3
}

internal class LocationPanelDiffCallback(
    private val oldList: List<LocationPanelUiModel>,
    private val newList: List<LocationPanelUiModel>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].locationData == newList[newItemPosition].locationData
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

abstract class LocationPanelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // Event listeners
    protected var onClickListener: ListAdapterOnClickInterface<LocationPanelUiModel>? = null
        private set

    fun setOnClickListener(onClickListener: ListAdapterOnClickInterface<LocationPanelUiModel>?) {
        this.onClickListener = onClickListener
    }

    @CallSuper
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemView is LocationPanel) {
            val model = getPanelUiModel(position)

            holder.itemView.post {
                updatePanelBackground(holder.itemView as LocationPanel, model, false)
            }
        }
    }

    final override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val imageUpdateOnly = payloads.contains(LocationPanelPayload.IMAGE_UPDATE)

        if (imageUpdateOnly) {
            if (holder.itemView is LocationPanel) {
                val model = getPanelUiModel(position)

                holder.itemView.post {
                    updatePanelBackground(holder.itemView as LocationPanel, model, true)
                }
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private fun updatePanelBackground(
        locPanel: LocationPanel,
        panelView: LocationPanelUiModel?,
        skipCache: Boolean
    ) {
        if (panelView?.imageData != null) {
            locPanel.setWeatherBackground(panelView, skipCache)
        } else {
            locPanel.clearBackground()
        }
    }

    abstract fun getPanelUiModel(position: Int): LocationPanelUiModel?
    abstract fun getViewPosition(item: LocationPanelUiModel?): Int
}