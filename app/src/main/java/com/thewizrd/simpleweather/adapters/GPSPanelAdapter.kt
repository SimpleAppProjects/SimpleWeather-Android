package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.LocationPanel
import com.thewizrd.simpleweather.controls.LocationPanelUiModel

class GPSPanelAdapter : LocationPanelAdapter() {
    private var uiModel: LocationPanelUiModel? = null

    fun updateModel(model: LocationPanelUiModel?) {
        if (uiModel != model) {
            val oldModel = this.uiModel
            this.uiModel = model

            if (oldModel == null && model != null) {
                notifyItemRangeInserted(0, 2)
            } else if (oldModel != null && model == null) {
                notifyItemRangeRemoved(0, 2)
            } else {
                notifyItemRangeChanged(0, 2)
            }
        }
    }

    inner class HeaderViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView), HeaderSetterInterface {
        private val header = itemView.findViewById<TextView>(R.id.header)

        override fun setHeader() {
            header.setText(R.string.label_currentlocation)
        }

        override fun setHeaderTextColor() {
            header.setTextColor(header.context.getAttrColor(android.R.attr.textColorPrimary))
        }
    }

    inner class ViewHolder internal constructor(private val locPanel: LocationPanel) :
        RecyclerView.ViewHolder(locPanel) {
        init {
            locPanel.showLoading(true)
        }

        fun bind(model: LocationPanelUiModel) {
            locPanel.bindModel(model)
            locPanel.setOnClickListener { v ->
                onClickListener?.onClick(v, model)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context

        return when (viewType) {
            LocationPanelItemType.HEADER_GPS -> {
                HeaderViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.locations_header, parent, false)
                )
            }
            else -> {
                // create a new view
                ViewHolder(LocationPanel(context))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderSetterInterface -> {
                holder.setHeader()
                holder.setHeaderTextColor()
            }
            else -> {
                val vHolder = holder as ViewHolder

                uiModel?.let { vHolder.bind(it) }
            }
        }

        super.onBindViewHolder(holder, position)
    }

    override fun getPanelUiModel(position: Int): LocationPanelUiModel? {
        if (position >= itemCount || position < 0)
            return null

        return if (itemCount > 0 && position == 0) {
            null
        } else {
            uiModel
        }
    }

    override fun getViewPosition(item: LocationPanelUiModel?): Int {
        return if (itemCount > 0 && item == uiModel) {
            1
        } else {
            RecyclerView.NO_POSITION
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && uiModel != null) {
            LocationPanelItemType.HEADER_GPS
        } else {
            LocationPanelItemType.GPS_PANEL
        }
    }

    override fun getItemCount(): Int {
        return if (uiModel == null) {
            0
        } else {
            2
        }
    }
}