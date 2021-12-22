package com.thewizrd.simpleweather.controls

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import kotlin.math.max
import kotlin.math.min

/**
 * FlowLayout is similar to [Flow] layout, except it extends the width of its child views to fill
 * the parent width.
 *
 * Attribute <i>layout_itemMinWidth</i> should be set for child views to display properly
 *
 * Based on my ExtendingWrapPanel implementation for UWP
 */
class FlowLayout : ViewGroup {
    private var lineSpacing = 0
    private var itemSpacing = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadFromAttributes(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        loadFromAttributes(context, attrs)
    }

    private fun loadFromAttributes(context: Context, attrs: AttributeSet?) {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.FlowLayout, 0, 0)
        lineSpacing = array.getDimensionPixelSize(R.styleable.FlowLayout_lineSpacing, 0)
        itemSpacing = array.getDimensionPixelSize(R.styleable.FlowLayout_itemSpacing, 0)
        array.recycle()
    }

    fun getLineSpacing(): Int {
        return lineSpacing
    }

    fun setLineSpacing(lineSpacing: Int) {
        this.lineSpacing = lineSpacing
        requestLayout()
    }

    fun getItemSpacing(): Int {
        return itemSpacing
    }

    fun setItemSpacing(itemSpacing: Int) {
        this.itemSpacing = itemSpacing
        requestLayout()
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return when (p) {
            is LayoutParams -> {
                LayoutParams(p)
            }
            is MarginLayoutParams -> {
                LayoutParams(p)
            }
            else -> {
                LayoutParams(p)
            }
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    inner class LayoutParams : MarginLayoutParams {
        internal var itemWidth: Int = 0
        internal var itemHeight: Int = 0

        internal var itemLeft: Int = 0
        internal var itemTop: Int = 0
        internal var itemRight: Int = 0
        internal var itemBottom: Int = 0

        var itemMinimumWidth: Int = 0

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_Layout)

            itemMinimumWidth = a.getDimensionPixelSize(R.styleable.FlowLayout_Layout_layout_itemMinWidth, 0)

            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(p: ViewGroup.LayoutParams) : super(p)

        constructor(source: MarginLayoutParams) : super(source)

        constructor(source: LayoutParams) : super(source) {
            this.itemWidth = source.itemWidth
            this.itemHeight = source.itemHeight
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        if (BuildConfig.DEBUG) {
            Log.d("FlowLayout", "onMeasure: specHeight = $height")
        }

        val maxWidth =
                if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) width else Int.MAX_VALUE

        val maxRight = maxWidth - paddingRight

        forEach {
            if (it.visibility != View.GONE) {
                val lp = it.layoutParams as LayoutParams
                it.measure(
                        getChildMeasureSpec(
                                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                                paddingLeft + paddingRight,
                                if (lp.itemMinimumWidth > 0) {
                                    lp.itemMinimumWidth
                                } else if (it.minimumWidth > 0) {
                                    it.minimumWidth
                                } else {
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                }
                        ),
                        MeasureSpec.makeMeasureSpec(
                                MeasureSpec.getSize(heightMeasureSpec),
                                MeasureSpec.UNSPECIFIED
                        )
                )

                if (BuildConfig.DEBUG) {
                    Log.d("FlowLayout", "onMeasure: child (id: ${it.id}) width = ${it.measuredWidth}; height = ${it.measuredHeight}")
                }
            }
        }

        val requiredSize = updateRows(paddingLeft, maxRight, widthMeasureSpec, heightMeasureSpec)

        val finalWidth = getMeasuredDimension(width, widthMode, requiredSize.width)
        val finalHeight = getMeasuredDimension(height, heightMode, requiredSize.height)
        setMeasuredDimension(finalWidth, finalHeight)

        if (BuildConfig.DEBUG) {
            Log.d("FlowLayout", "onMeasure: width = $finalWidth; height = $finalHeight")
        }
    }

    private fun getMeasuredDimension(size: Int, mode: Int, childrenEdge: Int): Int {
        return when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> Math.min(childrenEdge, size)
            else -> childrenEdge
        }
    }

    private class Row {
        var rowStart: Int = 0
        var rowTop: Int = 0
        var rowMaxBottom: Int = 0

        private val _items = mutableListOf<View>()

        val items: List<View>
            get() = _items

        constructor()
        constructor(rowStart: Int, rowTop: Int) {
            this.rowStart = rowStart
            this.rowTop = rowTop
        }

        fun add(child: View) {
            _items.add(child)
        }
    }

    private val mRows = mutableListOf<Row>()

    private fun updateRows(parentStart: Int, parentEnd: Int, parentWidthMeasureSpec: Int, parentHeightMeasureSpec: Int): Size {
        mRows.clear()

        val isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
        val paddingStart = if (isRtl) paddingRight else paddingLeft
        val paddingEnd = if (isRtl) paddingLeft else paddingRight
        val maxChildEnd = parentEnd - parentStart - paddingEnd

        val maxChildWidth = maxChildEnd - (parentStart + paddingStart)

        if (childCount == 0) {
            return Size(paddingStart + paddingEnd, paddingTop + paddingBottom)
        }

        var mCurrentRow = Row(paddingStart, paddingTop)
        var finalWidth = 0
        var finalHeight = 0

        fun layoutRowItems() {
            val initialFreeSpace = parentEnd - (itemSpacing * mCurrentRow.items.size) - paddingStart
            var freeSpace = initialFreeSpace
            var expandingItems = mCurrentRow.items.size

            // Reset starting point
            mCurrentRow.rowStart = paddingStart

            if (expandingItems == 1) {
                val item = mCurrentRow.items.first()

                val lp = item.layoutParams as LayoutParams
                val startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
                val endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)

                val desiredWidth = (parentEnd - endMargin) - (mCurrentRow.rowStart + startMargin)

                // Measure child
                val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        desiredWidth, MeasureSpec.EXACTLY
                )
                val childHeightMeasureSpec = getChildMeasureSpec(
                        parentHeightMeasureSpec,
                        paddingTop + paddingBottom,
                        lp.height
                )

                item.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                // rtl doesn't matter as its only item in row
                lp.itemLeft = mCurrentRow.rowStart + startMargin
                lp.itemTop = mCurrentRow.rowTop
                lp.itemRight = parentEnd - endMargin
                lp.itemBottom = mCurrentRow.rowTop + item.measuredHeight
            } else {
                var maxWidth = 0
                mCurrentRow.items.forEach {
                    val desiredWidth = it.measuredWidth
                    val lp = it.layoutParams

                    if (lp is LayoutParams) {
                        maxWidth = maxOf(desiredWidth, lp.itemMinimumWidth, lp.itemWidth)
                    }

                    if (desiredWidth > 0) {
                        freeSpace -= desiredWidth
                        expandingItems--
                    }
                }

                var fillSpace = false
                var uniformWidth = false
                if (expandingItems <= 0 && freeSpace > 0) {
                    expandingItems = mCurrentRow.items.size
                    fillSpace = true
                    if (maxWidth * mCurrentRow.items.size > 0) {
                        uniformWidth = true
                    }
                }

                mCurrentRow.items.forEach {
                    val lp = it.layoutParams as LayoutParams
                    val startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
                    val endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)
                    var desiredWidth = maxOf(it.measuredWidth, lp.itemWidth, lp.itemMinimumWidth)

                    if (desiredWidth == 0) {
                        return@forEach
                    }

                    if (fillSpace && freeSpace > 0 && expandingItems > 0) {
                        if (uniformWidth) {
                            desiredWidth = initialFreeSpace / mCurrentRow.items.size
                        } else {
                            desiredWidth += (freeSpace / expandingItems)
                        }
                    }

                    // Measure child
                    val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            desiredWidth, MeasureSpec.EXACTLY
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                            parentHeightMeasureSpec,
                            paddingTop + paddingBottom,
                            lp.height
                    )

                    it.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                    val childEnd = mCurrentRow.rowStart + startMargin + desiredWidth
                    val childBottom = mCurrentRow.rowTop + it.measuredHeight
                    val childTop = mCurrentRow.rowTop

                    if (isRtl) {
                        lp.itemLeft = maxChildEnd - childEnd
                        lp.itemTop = childTop
                        lp.itemRight = maxChildEnd - mCurrentRow.rowStart - startMargin
                        lp.itemBottom = childBottom
                    } else {
                        lp.itemLeft = mCurrentRow.rowStart + startMargin
                        lp.itemTop = childTop
                        lp.itemRight = childEnd
                        lp.itemBottom = childBottom
                    }

                    mCurrentRow.rowStart += (startMargin + endMargin + desiredWidth) + itemSpacing
                }
            }
        }

        fun layoutChild(child: View, isLast: Boolean = false) {
            if (child.visibility == GONE) {
                return // if an item is collapsed, avoid adding the spacing
            }

            val lp = child.layoutParams as LayoutParams
            val startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
            val endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)
            var desiredWidth = min(maxChildWidth, max(child.measuredWidth, lp.itemMinimumWidth))

            val childEnd = mCurrentRow.rowStart + startMargin + desiredWidth

            if (childEnd > maxChildEnd) {
                layoutRowItems()

                // next row
                mRows.add(mCurrentRow)
                mCurrentRow = Row(paddingStart, mCurrentRow.rowMaxBottom + lineSpacing)
            }

            mCurrentRow.rowMaxBottom = max(mCurrentRow.rowMaxBottom, mCurrentRow.rowTop + child.measuredHeight)

            // Stretch the last item to fill the available space
            if (isLast) {
                if (mCurrentRow.items.isEmpty()) {
                    desiredWidth = maxChildEnd - mCurrentRow.rowStart

                    val itemLp = child.layoutParams
                    if (itemLp is LayoutParams) {
                        itemLp.itemWidth = desiredWidth
                    }
                }

                mCurrentRow.add(child)
                layoutRowItems()
            } else {
                mCurrentRow.add(child)

                // Advance position
                mCurrentRow.rowStart += (startMargin + endMargin + desiredWidth) + itemSpacing
                finalWidth = max(finalWidth, mCurrentRow.rowStart)
            }
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            layoutChild(child, i == childCount - 1)
        }

        if (mCurrentRow.items.isNotEmpty()) {
            mRows.add(mCurrentRow)
        }

        if (mRows.isEmpty()) {
            return Size(paddingStart + paddingEnd, paddingTop + paddingBottom)
        }

        val lastRow = mRows.last()
        finalHeight = lastRow.rowMaxBottom
        return Size(finalWidth + paddingEnd, finalHeight + paddingBottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount == 0) {
            // Do not re-layout when there are no children.
            return
        }

        if (mRows.isNotEmpty()) {
            mRows.forEach { row ->
                row.items.forEach { child ->
                    if (child.visibility != View.GONE) {
                        val lp = child.layoutParams as LayoutParams
                        child.layout(
                                lp.itemLeft,
                                lp.itemTop,
                                lp.itemRight,
                                lp.itemBottom
                        )
                    }
                }
            }
        }
    }
}