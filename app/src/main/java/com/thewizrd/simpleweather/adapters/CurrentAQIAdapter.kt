package com.thewizrd.simpleweather.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.view.updateMarginsRelative
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.common.controls.AirQualityViewModel
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LayoutAqiForecastItemBinding

class CurrentAQIAdapter : RecyclerView.Adapter<CurrentAQIAdapter.ViewHolder>() {
    companion object {
        private const val CORNERS_FULL = 0
        private const val CORNERS_TOP = 1
        private const val CORNERS_CENTER = 2
        private const val CORNERS_BOTTOM = 3
    }

    @IntDef(value = [CORNERS_FULL, CORNERS_TOP, CORNERS_CENTER, CORNERS_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    private annotation class CornersType

    private var aqiModel: AirQualityViewModel? = null

    inner class ViewHolder(private val binding: LayoutAqiForecastItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context = itemView.context

        fun bindModel(aqiData: AirQualityViewModel?) {
            binding.viewModel = aqiData
            binding.executePendingBindings()
            binding.aqiDateLabel.text = context.getString(sharedRes.string.time_current)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutAqiForecastItemBinding.inflate(LayoutInflater.from(parent.context)).apply {
            this.root.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                val horizontalMargin = parent.context.dpToPx(6f).toInt()
                val verticalMargin = parent.context.dpToPx(2f).toInt()
                updateMarginsRelative(
                    start = horizontalMargin,
                    end = horizontalMargin,
                    top = verticalMargin,
                    bottom = verticalMargin
                )
            }
        })
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindModel(aqiModel)

        if (holder.itemView is MaterialCardView) {
            updateItemCorners(holder.itemView as MaterialCardView, getCornersType(position))
        }
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

    @CornersType
    private fun getCornersType(position: Int): Int {
        return when {
            itemCount <= 1 -> CORNERS_FULL
            position == 0 -> CORNERS_TOP
            position == itemCount - 1 -> CORNERS_BOTTOM
            else -> CORNERS_CENTER
        }
    }

    private fun updateItemCorners(cardView: MaterialCardView, @CornersType cornersType: Int) {
        val baseShapeModel =
            ShapeAppearanceModel.builder(cardView.context, null, 0, R.style.WeatherNow_CardView)
        val smallCornerSize = cardView.context.dpToPx(8f)

        when (cornersType) {
            CORNERS_BOTTOM -> {
                baseShapeModel.setTopLeftCornerSize(smallCornerSize)
                baseShapeModel.setTopRightCornerSize(smallCornerSize)
            }

            CORNERS_CENTER -> {
                baseShapeModel.setTopLeftCornerSize(smallCornerSize)
                baseShapeModel.setTopRightCornerSize(smallCornerSize)
                baseShapeModel.setBottomLeftCornerSize(smallCornerSize)
                baseShapeModel.setBottomRightCornerSize(smallCornerSize)
            }

            CORNERS_FULL -> {}

            CORNERS_TOP -> {
                baseShapeModel.setBottomLeftCornerSize(smallCornerSize)
                baseShapeModel.setBottomRightCornerSize(smallCornerSize)
            }
        }

        cardView.shapeAppearanceModel = baseShapeModel.build()
    }
}