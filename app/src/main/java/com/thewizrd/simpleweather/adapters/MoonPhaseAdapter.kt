package com.thewizrd.simpleweather.adapters

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.core.view.updateMarginsRelative
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.common.controls.IconControl
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase.MoonPhaseType

class MoonPhaseAdapter : RecyclerView.Adapter<MoonPhaseAdapter.MoonPhaseViewHolder>() {
    private val dataset = MoonPhaseType.entries.toList()
    var selectedMoonPhaseType = MoonPhaseType.FULL_MOON
        private set
    private val selectedIndex
        get() = selectedMoonPhaseType.ordinal + MoonPhaseType.entries.size

    private val selectPayload: Any = "SELECT_PAYLOAD"

    init {
        setHasStableIds(true)
    }

    inner class MoonPhaseViewHolder(val view: IconControl) : RecyclerView.ViewHolder(view) {
        fun bind(phaseType: MoonPhaseType, isSelected: Boolean = false) {
            view.weatherIcon = when (phaseType) {
                MoonPhaseType.NEWMOON -> WeatherIcons.MOON_NEW
                MoonPhaseType.WAXING_CRESCENT -> WeatherIcons.MOON_WAXING_CRESCENT_3
                MoonPhaseType.FIRST_QTR -> WeatherIcons.MOON_FIRST_QUARTER
                MoonPhaseType.WAXING_GIBBOUS -> WeatherIcons.MOON_WAXING_GIBBOUS_3
                MoonPhaseType.FULL_MOON -> WeatherIcons.MOON_FULL
                MoonPhaseType.WANING_GIBBOUS -> WeatherIcons.MOON_WANING_GIBBOUS_3
                MoonPhaseType.LAST_QTR -> WeatherIcons.MOON_THIRD_QUARTER
                MoonPhaseType.WANING_CRESCENT -> WeatherIcons.MOON_WANING_CRESCENT_3
            }

            setSelected(isSelected)
        }

        fun setSelected(isSelected: Boolean) {
            view.alpha = if (isSelected) {
                1.0f
            } else {
                when (bindingAdapterPosition) {
                    selectedIndex - 1, selectedIndex + 1 -> 0.35f
                    selectedIndex - 2, selectedIndex + 2 -> 0.20f
                    selectedIndex - 3, selectedIndex + 3 -> 0.15f
                    else -> 0.35f
                }
            }
            view.shouldAnimate = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoonPhaseViewHolder {
        return MoonPhaseViewHolder(
            IconControl(parent.context).apply {
                val size = context.dpToPx(60f).toInt()
                val margin = context.dpToPx(4f).toInt()

                layoutParams = RecyclerView.LayoutParams(size, size).apply {
                    updateMarginsRelative(start = margin, end = margin)
                }

                cropToPadding = false
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(context.dpToPx(4f).toInt())
            }
        )
    }

    override fun getItemCount(): Int {
        return dataset.size * 3
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: MoonPhaseViewHolder, position: Int) {
        val phase = dataset[position % dataset.size]
        holder.bind(phase, phase == selectedMoonPhaseType)
    }

    override fun onBindViewHolder(
        holder: MoonPhaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(selectPayload)) {
            holder.setSelected(dataset[position % dataset.size] == selectedMoonPhaseType)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun selectMoonPhase(phaseType: MoonPhaseType) {
        selectedMoonPhaseType = phaseType
        notifyItemRangeChanged(0, itemCount, selectPayload)
    }

    override fun onViewRecycled(holder: MoonPhaseViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.cancelPendingInputEvents()
        holder.itemView.animate().cancel()
    }
}