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
import androidx.annotation.Px
import androidx.core.view.ViewGroupCompat
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.simpleweather.R

class RangeBarGraphPanel : LinearLayout, GraphPanel {
    private lateinit var barChartView: RangeBarGraphView

    private var graphData: RangeBarGraphData? = null

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
        barChartView = RangeBarGraphView(context)

        val lineViewHeight = context.resources.getDimensionPixelSize(R.dimen.forecast_panel_height)
        val onTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (v is IGraph) {
                    onClickListener?.onClick(v, v.getItemPositionFromPoint(event.x))
                }
                return@OnTouchListener true
            }
            false
        }
        barChartView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight).apply {
            gravity = Gravity.CENTER
        }
        barChartView.setOnTouchListener(onTouchListener)
        barChartView.setDrawDataLabels(true)
        barChartView.setDrawIconLabels(true)
        //barChartView.setGraphMaxWidth(context.resources.getDimensionPixelSize(R.dimen.graph_max_width))

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

    fun setGraphData(data: RangeBarGraphData?) {
        this.graphData = data
        barChartView.data = data
        resetView()
    }

    override fun setDrawIconLabels(drawIconsLabels: Boolean) {
        barChartView.setDrawIconLabels(drawIconsLabels)
    }

    override fun setDrawDataLabels(drawDataLabels: Boolean) {
        barChartView.setDrawDataLabels(drawDataLabels)
    }

    override fun setScrollingEnabled(enabled: Boolean) {
        barChartView.isScrollingEnabled = enabled
    }

    override fun getItemPositionFromPoint(xCoordinate: Float): Int {
        return barChartView.getItemPositionFromPoint(xCoordinate)
    }

    override fun setBottomTextSize(@Px textSize: Float) {
        barChartView.setBottomTextSize(textSize)
    }

    override fun setIconSize(iconSize: Float) {
        barChartView.setIconSize(iconSize)
    }

    override fun setGraphMaxWidth(maxWidth: Int) {
        barChartView.setGraphMaxWidth(maxWidth)
    }

    override fun setFillParentWidth(fillParentWidth: Boolean) {
        barChartView.setFillParentWidth(fillParentWidth)
    }

    override fun setBottomTextColor(color: Int) {
        barChartView.setBottomTextColor(color)
    }

    override fun requestGraphLayout() {
        barChartView.requestGraphLayout()
    }
}