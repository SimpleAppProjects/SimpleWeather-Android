/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thewizrd.simpleweather.controls

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat
import com.thewizrd.simpleweather.R
import kotlin.math.max

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

        val maxWidth =
                if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) width else Int.MAX_VALUE

        var childLeft = paddingLeft
        var childTop = paddingTop
        var childBottom = childTop
        var childRight = childLeft
        var maxChildRight = 0
        val maxRight = maxWidth - paddingRight
        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child.visibility == GONE) {
                continue
            }

            val lp = child.layoutParams
            var leftMargin = 0
            var rightMargin = 0
            var desiredWidth = child.measuredWidth
            if (lp is MarginLayoutParams) {
                leftMargin += lp.leftMargin
                rightMargin += lp.rightMargin
            }
            if (lp is LayoutParams) {
                desiredWidth = max(desiredWidth, lp.itemMinimumWidth)
            }

            val childWidthMeasureSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    paddingStart + paddingEnd,
                    desiredWidth
            )
            val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    paddingTop + paddingBottom, lp.height)

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)

            childRight = childLeft + leftMargin + desiredWidth

            // If the current child's right bound exceeds Flowlayout's max right bound and flowlayout is
            // not confined to a single line, move this child to the next line and reset its left bound to
            // flowlayout's left bound.
            if (childRight > maxRight) {
                childLeft = paddingLeft
                childTop = childBottom + lineSpacing
            }

            childRight = childLeft + leftMargin + desiredWidth
            childBottom = childTop + child.measuredHeight

            // Updates Flowlayout's max right bound if current child's right bound exceeds it.
            if (childRight > maxChildRight) {
                maxChildRight = childRight
            }

            childLeft += leftMargin + rightMargin + desiredWidth + itemSpacing

            // For all preceding children, the child's right margin is taken into account in the next
            // child's left bound (childLeft). However, childLeft is ignored after the last child so the
            // last child's right margin needs to be explicitly added to Flowlayout's max right bound.
            if (i == childCount - 1) {
                maxChildRight += rightMargin
            }
        }

        maxChildRight += paddingRight
        childBottom += paddingBottom

        val finalWidth = getMeasuredDimension(width, widthMode, maxChildRight)
        val finalHeight = getMeasuredDimension(height, heightMode, childBottom)
        setMeasuredDimension(finalWidth, finalHeight)
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

        val items = mutableListOf<View>()

        fun reset(positionStart: Int, positionTop: Int) {
            rowStart = positionStart
            rowTop = positionTop
            rowMaxBottom = 0

            items.clear()
        }
    }

    private val mCurrentRow = Row()
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount == 0) {
            // Do not re-layout when there are no children.
            return
        }

        val isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
        val paddingStart = if (isRtl) paddingRight else paddingLeft
        val paddingEnd = if (isRtl) paddingLeft else paddingRight
        val maxChildEnd = right - left - paddingEnd

        mCurrentRow.reset(paddingStart, paddingTop)

        fun layoutRowItems() {
            val initialFreeSpace = maxChildEnd - (itemSpacing * mCurrentRow.items.size) - paddingStart
            var freeSpace = initialFreeSpace
            var expandingItems = mCurrentRow.items.size

            if (expandingItems == 1) {
                val item = mCurrentRow.items.first()

                val lp = item.layoutParams
                var startMargin = 0
                var endMargin = 0
                if (lp is MarginLayoutParams) {
                    startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
                    endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)
                }

                // Re-measure children
                val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        ((maxChildEnd - endMargin) - (mCurrentRow.rowStart + startMargin)), MeasureSpec.EXACTLY
                )
                val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        item.measuredHeight, MeasureSpec.AT_MOST
                )

                item.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                // rtl doesn't matter as its only item in row
                item.layout(mCurrentRow.rowStart + startMargin, mCurrentRow.rowTop, maxChildEnd - endMargin, mCurrentRow.rowTop + item.measuredHeight)
            } else {
                mCurrentRow.rowStart = paddingStart

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
                    var desiredWidth = it.measuredWidth

                    val lp = it.layoutParams
                    var startMargin = 0
                    var endMargin = 0
                    if (lp is MarginLayoutParams) {
                        startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
                        endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)
                    }
                    if (lp is LayoutParams) {
                        desiredWidth = maxOf(desiredWidth, lp.itemWidth, lp.itemMinimumWidth)
                    }

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

                    val childEnd = mCurrentRow.rowStart + startMargin + desiredWidth
                    val childBottom = mCurrentRow.rowTop + it.measuredHeight
                    val childTop = mCurrentRow.rowTop

                    val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            desiredWidth, MeasureSpec.EXACTLY
                    )
                    val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            it.measuredHeight, MeasureSpec.UNSPECIFIED
                    )

                    it.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                    if (isRtl) {
                        it.layout(
                                maxChildEnd - childEnd,
                                mCurrentRow.rowTop,
                                maxChildEnd - mCurrentRow.rowStart - startMargin,
                                childBottom
                        )
                    } else {
                        it.layout(mCurrentRow.rowStart + startMargin, mCurrentRow.rowTop, childEnd, childBottom)
                    }

                    mCurrentRow.rowStart += (startMargin + endMargin + desiredWidth) + itemSpacing
                }
            }
        }

        fun layoutChild(child: View, isLast: Boolean = false) {
            if (child.visibility == GONE) {
                return // if an item is collapsed, avoid adding the spacing
            }

            val lp = child.layoutParams
            var startMargin = 0
            var endMargin = 0
            var desiredWidth = child.measuredWidth
            if (lp is MarginLayoutParams) {
                startMargin = MarginLayoutParamsCompat.getMarginStart(lp)
                endMargin = MarginLayoutParamsCompat.getMarginEnd(lp)
            }
            if (lp is LayoutParams) {
                desiredWidth = max(desiredWidth, lp.itemMinimumWidth)
            }

            val childEnd = mCurrentRow.rowStart + startMargin + desiredWidth

            if (childEnd > maxChildEnd) {
                layoutRowItems()

                // next row
                mCurrentRow.reset(paddingStart, mCurrentRow.rowMaxBottom + lineSpacing)

                // clear row items
                mCurrentRow.items.clear()
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

                mCurrentRow.items.add(child)
                layoutRowItems()
            } else {
                mCurrentRow.items.add(child)

                // Advance position
                mCurrentRow.rowStart += (startMargin + endMargin + desiredWidth) + itemSpacing
            }
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            layoutChild(child, i == childCount - 1)
        }
    }
}