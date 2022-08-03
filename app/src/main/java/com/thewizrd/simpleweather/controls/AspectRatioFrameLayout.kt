package com.thewizrd.simpleweather.controls

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.thewizrd.simpleweather.R

class AspectRatioFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private var mAspectRatio = 1f
    private var mEnableAspectRatio = false

    private var mMaxHeight = Int.MAX_VALUE
    private var mMaxWidth = Int.MAX_VALUE

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout)

        mAspectRatio = a.getFloat(R.styleable.AspectRatioFrameLayout_aspectRatio, mAspectRatio)
        mEnableAspectRatio =
            a.getBoolean(R.styleable.AspectRatioFrameLayout_enableAspectRatio, mEnableAspectRatio)

        mMaxHeight =
            a.getDimensionPixelSize(R.styleable.AspectRatioFrameLayout_maxHeight, mMaxHeight)
        mMaxWidth = a.getDimensionPixelSize(R.styleable.AspectRatioFrameLayout_maxHeight, mMaxWidth)

        a.recycle()
    }

    /** Sets the desired aspect ratio (w/h).  */
    fun setAspectRatio(aspectRatio: Float) {
        if (aspectRatio == mAspectRatio) {
            return
        }
        mAspectRatio = aspectRatio
        requestLayout()
    }

    /** Gets the desired aspect ratio (w/h).  */
    fun getAspectRatio(): Float {
        return mAspectRatio
    }

    fun isAspectRatioEnabled(): Boolean {
        return mEnableAspectRatio
    }

    fun setAspectRatioEnabled(enable: Boolean) {
        if (enable != mEnableAspectRatio) {
            mEnableAspectRatio = enable
            requestLayout()
        }
    }

    var maxHeight: Int
        get() = mMaxHeight
        set(value) {
            if (value != mMaxHeight) {
                mMaxHeight = value
                requestLayout()
            }
        }

    var maxWidth: Int
        get() = mMaxWidth
        set(value) {
            if (value != mMaxWidth) {
                mMaxWidth = value
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec
        val widthPadding = paddingLeft + paddingRight
        val heightPadding = paddingTop + paddingBottom

        if (mEnableAspectRatio && mAspectRatio > 0 && layoutParams != null) {
            if (layoutParams.height == 0 || layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = obtainWidthMeasureSpec(widthSpec)
                val widthSpecSize = MeasureSpec.getSize(widthSpec)
                val desiredHeight =
                    ((widthSpecSize - widthPadding) / mAspectRatio + heightPadding).toInt()
                val resolvedHeight = resolveSize(desiredHeight, heightSpec)
                heightSpec = MeasureSpec.makeMeasureSpec(resolvedHeight, MeasureSpec.EXACTLY)
            } else if (layoutParams.width == 0 || layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = obtainHeightMeasureSpec(heightSpec)
                val heightSpecSize = MeasureSpec.getSize(heightSpec)
                val desiredWidth =
                    ((heightSpecSize - heightPadding) * mAspectRatio + widthPadding).toInt()
                val resolvedWidth = resolveSize(desiredWidth, widthSpec)
                widthSpec = MeasureSpec.makeMeasureSpec(resolvedWidth, MeasureSpec.EXACTLY)
            }
        } else {
            heightSpec = obtainHeightMeasureSpec(heightSpec)
            widthSpec = obtainWidthMeasureSpec(widthSpec)
        }

        super.onMeasure(widthSpec, heightSpec)
    }

    private fun obtainHeightMeasureSpec(heightMeasureSpec: Int): Int {
        return if (mMaxHeight > 0 && mMaxHeight != Int.MAX_VALUE) {
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
            val resolvedHeight = resolveSize(heightSpecSize, heightMeasureSpec)
            obtainMeasureSpec(minimumHeight, mMaxHeight, resolvedHeight)
        } else {
            heightMeasureSpec
        }
    }

    private fun obtainWidthMeasureSpec(widthMeasureSpec: Int): Int {
        return if (mMaxWidth > 0 && mMaxWidth != Int.MAX_VALUE) {
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val resolvedWidth = resolveSize(widthSpecSize, widthMeasureSpec)
            obtainMeasureSpec(minimumWidth, mMaxWidth, resolvedWidth)
        } else {
            widthMeasureSpec
        }
    }

    private fun obtainMeasureSpec(
        min: Int,
        max: Int,
        preferred: Int
    ): Int = when {
        preferred >= 0 || min == max -> {
            // Fixed size due to fixed size layout param or fixed constraints.
            MeasureSpec.makeMeasureSpec(preferred.coerceIn(min, max), MeasureSpec.EXACTLY)
        }
        preferred == ViewGroup.LayoutParams.WRAP_CONTENT && max != Int.MAX_VALUE -> {
            // Wrap content layout param with finite max constraint. If max constraint is infinite,
            // we will measure the child with UNSPECIFIED.
            MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST)
        }
        preferred == ViewGroup.LayoutParams.MATCH_PARENT && max != Int.MAX_VALUE -> {
            // Match parent layout param, so we force the child to fill the available space.
            MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY)
        }
        else -> {
            // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        }
    }
}