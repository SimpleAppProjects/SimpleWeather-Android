package com.thewizrd.simpleweather.controls.graphs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.LinearLayout
import androidx.core.view.ViewGroupCompat
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.simpleweather.R

class BarGraphPanel : LinearLayout {
    private lateinit var barChartView: BarGraphView

    private var graphData: BarGraphData? = null

    private lateinit var currentConfig: Configuration

    // Event listeners
    private var onClickListener: RecyclerOnClickListenerInterface? = null

    fun setOnClickPositionListener(onClickListener: RecyclerOnClickListenerInterface?) {
        this.onClickListener = onClickListener
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
            context,
            attrs,
            defStyleAttr,
            0
    )

    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(context)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        currentConfig = Configuration(newConfig)

        updateViewColors()

        barChartView.postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize(context: Context) {
        currentConfig = Configuration(context.resources.configuration)
        orientation = VERTICAL
        barChartView = BarGraphView(context)

        val graphPanelHeight = context.resources.getDimensionPixelSize(R.dimen.bargraph_panel_height)
        val onTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (v is IGraph) {
                    onClickListener?.onClick(v, v.getItemPositionFromPoint(event.x))
                }
                return@OnTouchListener true
            }
            false
        }
        barChartView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, graphPanelHeight).apply {
            gravity = Gravity.CENTER
        }
        barChartView.setOnTouchListener(onTouchListener)
        barChartView.setDrawDataLabels(true)
        barChartView.setDrawIconLabels(true)

        removeAllViews()
        this.addView(barChartView)

        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true)

        resetView()
    }

    private fun updateViewColors() {
        barChartView.setBottomTextColor(context.getAttrColor(android.R.attr.textColorPrimary))
    }

    private fun resetView() {
        updateViewColors()
        barChartView.resetData(false)
        updateGraphData()
    }

    private fun updateGraphData() {
        barChartView.data = graphData
    }

    fun setGraphData(data: BarGraphData?) {
        this.graphData = data
        barChartView.data = data
        resetView()
    }

    fun setMaxWidth(maxWidth: Int) {
        barChartView.setGraphMaxWidth(maxWidth)
    }

    fun setDrawIconLabels(enable: Boolean) {
        barChartView.setDrawIconLabels(enable)
    }

    fun setDrawDataLabels(enable: Boolean) {
        barChartView.setDrawDataLabels(enable)
    }
}