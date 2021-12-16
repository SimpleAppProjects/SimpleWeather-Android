package com.thewizrd.simpleweather.controls

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.VirtualLayout
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet

class WrapLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private lateinit var mFlowLayout: Flow

    init {
        initialize(context)
    }

    private fun initialize(context: Context) {
        mFlowLayout = Flow(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.WRAP_CONTENT).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }

            setWrapMode(Flow.WRAP_CHAIN)
            setOrientation(Flow.HORIZONTAL)
            setHorizontalStyle(Flow.CHAIN_SPREAD_INSIDE)
            setHorizontalAlign(Flow.HORIZONTAL_ALIGN_START)
            setVerticalAlign(Flow.VERTICAL_ALIGN_CENTER)
            //setLastHorizontalStyle(Flow.CHAIN_SPREAD)
            //setLastHorizontalBias(0.5f)
            setHorizontalGap(context.dpToPx(4f).toInt())
            setVerticalGap(context.dpToPx(8f).toInt())
            if (!context.isLargeTablet()) {
                setMaxElementsWrap(1)
            }
        }

        addView(mFlowLayout)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child != null) {
            if (child.id == View.NO_ID) {
                Log.i("WrapLayout", "child does not have an id; generating a new id")
                child.id = generateViewId()
            }
        }
        super.addView(child, index, params)
        if (child != null && child !is VirtualLayout) {
            mFlowLayout.addView(child)
        }
    }
}