package com.thewizrd.simpleweather.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.view.updateMarginsRelative
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.common.adapters.LocationQueryDiffer
import com.thewizrd.common.databinding.LocationQueryViewBinding
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R

class LocationQueryAdapter :
    ListAdapter<LocationQuery, LocationQueryAdapter.ViewHolder>(LocationQueryDiffer()) {
    companion object {
        private const val CORNERS_FULL = 0
        private const val CORNERS_TOP = 1
        private const val CORNERS_CENTER = 2
        private const val CORNERS_BOTTOM = 3
    }

    @IntDef(value = [CORNERS_FULL, CORNERS_TOP, CORNERS_CENTER, CORNERS_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    private annotation class CornersType
        
    private var onClickListener: ListAdapterOnClickInterface<LocationQuery>? = null

    fun setOnClickListener(onClickListener: ListAdapterOnClickInterface<LocationQuery>?) {
        this.onClickListener = onClickListener
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(private val binding: LocationQueryViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: LocationQuery) {
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
            ).apply {
                val verticalMargin = parent.context.dpToPx(1f).toInt()
                updateMarginsRelative(top = verticalMargin, bottom = verticalMargin)
            }
        }
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        if (holder.itemView is MaterialCardView) {
            updateItemCorners(holder.itemView as MaterialCardView, getCornersType(position))
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
        val baseShapeModel = ShapeAppearanceModel.builder(
            cardView.context,
            com.google.android.material.R.style.ShapeAppearance_Material3_Corner_Large,
            0
        )
        val smallCornerSize = cardView.context.dpToPx(4f)

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