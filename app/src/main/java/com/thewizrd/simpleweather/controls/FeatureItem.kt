package com.thewizrd.simpleweather.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.google.android.material.R as materialRes

class FeatureItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), Checkable {
    companion object {
        private val CHECKABLE_STATE_SET = intArrayOf(android.R.attr.state_checkable)
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

    private val titleTextView: TextView
    private val dragHandle: View

    var isCheckable: Boolean = true
        set(value) {
            field = value
            refreshDrawableState()
        }

    var isDraggable: Boolean = true
        set(value) {
            field = value
            dragHandle.isVisible = value
        }

    private var _isChecked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_featureitem, this, true)
        layoutParams = ViewGroup.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        background = RippleDrawable(
            ColorStateList.valueOf(context.getAttrColor(android.R.attr.colorControlHighlight)),
            MaterialShapeDrawable().apply {
                fillColor = ColorStateList.valueOf(
                    if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                        ColorUtils.compositeColors(
                            context.getAttrColor(materialRes.attr.colorSurfaceDim),
                            Colors.BLACK
                        )
                    } else {
                        context.getAttrColor(materialRes.attr.colorSurfaceBright)
                    }
                )
                initializeElevationOverlay(context)
                elevation = 0f
                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    context,
                    materialRes.style.ShapeAppearance_Material3_Corner_LargeIncreased,
                    0
                ).build()
            },
            null
        )

        titleTextView = findViewById(R.id.title)
        dragHandle = findViewById(R.id.drag_handle)

        // Expressive styling
        minimumHeight = context.dpToPx(56f).toInt()
        val horizontalPadding =
            context.resources.getDimensionPixelSize(R.dimen.preference_expressive_space_small1)
        updatePaddingRelative(start = horizontalPadding, end = horizontalPadding)
        clipToPadding = false
        isBaselineAligned = false
        filterTouchesWhenObscured = false
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        dragHandle.setOnLongClickListener(l)
    }

    fun setTitle(text: CharSequence) {
        titleTextView.text = text
    }

    fun setTitle(@StringRes resId: Int) {
        titleTextView.setText(resId)
    }

    override fun setChecked(checked: Boolean) {
        _isChecked = checked
        refreshDrawableState()
    }

    override fun isChecked(): Boolean {
        return _isChecked
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 3)

        if (isCheckable) {
            mergeDrawableStates(drawableState, CHECKABLE_STATE_SET)
        }

        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }

        if (isEnabled) {
            mergeDrawableStates(drawableState, ENABLED_STATE_SET)
        }

        return drawableState
    }

    override fun setElevation(elevation: Float) {
        super.setElevation(elevation)
        if (background is MaterialShapeDrawable) {
            (background as? MaterialShapeDrawable)?.elevation = elevation
        } else if (background is RippleDrawable) {
            (background as? RippleDrawable)?.let { rippleDrawable ->
                for (i in 0 until rippleDrawable.numberOfLayers) {
                    val drawable = rippleDrawable.getDrawable(i)
                    if (drawable is MaterialShapeDrawable) {
                        drawable.elevation = elevation
                        break
                    }
                }
            }
        }
    }
}