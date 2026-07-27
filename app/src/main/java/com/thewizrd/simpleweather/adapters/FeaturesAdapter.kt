package com.thewizrd.simpleweather.adapters

import android.content.Context
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.common.R as commonRes
import com.thewizrd.common.helpers.ObservableArrayList
import com.thewizrd.common.helpers.OnListChangedListener
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.FeatureItem
import com.thewizrd.simpleweather.helpers.ItemTouchHelperAdapter
import com.thewizrd.simpleweather.preferences.FeatureSettings
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_AQINDEX
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_BEAUFORT
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_BGIMAGE
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_CHARTS
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_DETAILS
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_FORECAST
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_HRFORECAST
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_LOCPANELIMG
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_MOONPHASE
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_POLLEN
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_RADAR
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_SUMMARY
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_SUNPHASE
import com.thewizrd.simpleweather.preferences.FeatureSettings.KEY_FEATURE_UV
import kotlin.intArrayOf

internal class FeatureItemDiffCallback(
    private val oldList: List<String>,
    private val newList: List<String>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

class FeaturesAdapter : RecyclerView.Adapter<FeaturesAdapter.ViewHolder>(), ItemTouchHelperAdapter {
    companion object {
        val PREFKEY_TO_STRINGRES_MAP = mapOf(
            KEY_FEATURE_BGIMAGE to R.string.pref_title_feature_bgimage,
            KEY_FEATURE_FORECAST to R.string.pref_title_feature_forecast,
            KEY_FEATURE_HRFORECAST to R.string.pref_title_feature_hrforecast,
            KEY_FEATURE_CHARTS to R.string.pref_title_feature_charts,
            KEY_FEATURE_SUMMARY to R.string.pref_title_feature_summary,
            KEY_FEATURE_DETAILS to R.string.pref_title_feature_details,
            KEY_FEATURE_UV to R.string.pref_title_feature_uv,
            KEY_FEATURE_BEAUFORT to R.string.pref_title_feature_beaufort,
            KEY_FEATURE_AQINDEX to R.string.pref_title_feature_aqindex,
            KEY_FEATURE_POLLEN to sharedRes.string.label_pollen_count,
            KEY_FEATURE_MOONPHASE to R.string.pref_title_feature_moonphase,
            KEY_FEATURE_SUNPHASE to R.string.pref_title_feature_sunphase,
            KEY_FEATURE_RADAR to R.string.pref_title_feature_radar,
            KEY_FEATURE_LOCPANELIMG to R.string.pref_title_feature_locpanelimg,
        )

        val ORDERABLE_ITEMS = setOf(
            KEY_FEATURE_FORECAST,
            KEY_FEATURE_HRFORECAST,
            KEY_FEATURE_CHARTS,
            KEY_FEATURE_DETAILS,
            KEY_FEATURE_UV,
            KEY_FEATURE_BEAUFORT,
            KEY_FEATURE_AQINDEX,
            KEY_FEATURE_POLLEN,
            KEY_FEATURE_MOONPHASE,
            KEY_FEATURE_SUNPHASE,
            KEY_FEATURE_RADAR
        )

        val NON_ORDERABLE_ITEMS = setOf(
            KEY_FEATURE_BGIMAGE,
            KEY_FEATURE_SUMMARY,
            KEY_FEATURE_LOCPANELIMG
        )

        private const val CORNERS_FULL = 0
        private const val CORNERS_TOP = 1
        private const val CORNERS_CENTER = 2
        private const val CORNERS_BOTTOM = 3
    }

    @IntDef(value = [CORNERS_FULL, CORNERS_TOP, CORNERS_CENTER, CORNERS_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    private annotation class CornersType

    private val dataset = ObservableArrayList<String>()

    private var onLongClickToDragListener: ViewHolderLongClickListener? = null
    private var onListChangedCallback: OnListChangedListener<String>? = null

    fun setOnLongClickToDragListener(onLongClickToDragListener: ViewHolderLongClickListener?) {
        this.onLongClickToDragListener = onLongClickToDragListener
    }

    fun setOnListChangedCallback(onListChangedCallback: OnListChangedListener<String>?) {
        this.onListChangedCallback?.let { dataset.removeOnListChangedCallback(it) }

        this.onListChangedCallback = onListChangedCallback

        onListChangedCallback?.let { dataset.addOnListChangedCallback(it) }
    }

    fun updateList(list: List<String>) {
        val oldList = dataset.toList()

        val diffCallback = FeatureItemDiffCallback(oldList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        dataset.clear()
        dataset.addAll(list)

        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder internal constructor(private val item: FeatureItem) :
        RecyclerView.ViewHolder(item) {
        fun bind(key: String) {
            item.setTitle(PREFKEY_TO_STRINGRES_MAP.getValue(key))
            item.isDraggable = ORDERABLE_ITEMS.contains(key)
            item.isChecked = FeatureSettings.isFeatureEnabled(key)

            item.setOnClickListener {
                item.toggle()
                FeatureSettings.setFeatureEnabled(key, item.isChecked)
            }

            item.setOnLongClickListener {
                onLongClickToDragListener?.onLongClick(this)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FeatureItem(parent.context).apply {
            val horizontalMargins = parent.context.dpToPx(16f).toInt()
            val verticalMargins = parent.context.dpToPx(1f).toInt()
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = horizontalMargins
                rightMargin = horizontalMargins
                topMargin = verticalMargins
                bottomMargin = verticalMargins
            }
        })
    }

    override fun getItemCount(): Int = dataset.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataset[position])
        if (holder.itemView.background is RippleDrawable) {
            updateItemCorners(
                holder.itemView.context,
                holder.itemView.background as RippleDrawable,
                getCornersType(position)
            )
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        dataset.move(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        // Required for expressive corners
        notifyItemChanged(fromPosition)
        notifyItemChanged(toPosition)
    }

    override fun onItemDismiss(position: Int) { /* no-op */
    }

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        notifyItemChanged(viewHolder.bindingAdapterPosition)
    }

    @CornersType
    private fun getCornersType(position: Int): Int {
        return when {
            itemCount <= 1 && position == 0 -> CORNERS_FULL
            position == 0 -> CORNERS_TOP
            position == itemCount - 1 -> CORNERS_BOTTOM
            else -> CORNERS_CENTER
        }
    }

    private fun updateItemCorners(
        context: Context,
        background: RippleDrawable,
        @CornersType cornersType: Int
    ) {
        val baseShapeModel = ShapeAppearanceModel.builder(
            context,
            com.google.android.material.R.style.ShapeAppearance_Material3_Corner_Large,
            0
        )
        val smallCornerSize = context.dpToPx(4f)

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

        for (i in 0 until background.numberOfLayers) {
            val drawable = background.getDrawable(i)
            if (drawable is MaterialShapeDrawable) {
                drawable.shapeAppearanceModel = baseShapeModel.build()
                break
            }
        }
    }
}