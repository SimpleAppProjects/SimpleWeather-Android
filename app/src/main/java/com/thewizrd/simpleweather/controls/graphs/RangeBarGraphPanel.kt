package com.thewizrd.simpleweather.controls.graphs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.view.ViewGroupCompat
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.R

class RangeBarGraphPanel : LinearLayout {
    private lateinit var barChartView: RangeBarGraphView

    private var graphData: RangeBarGraphData? = null

    private lateinit var currentConfig: Configuration

    // Event listeners
    private var onClickListener: RecyclerOnClickListenerInterface? = null

    fun setOnClickPositionListener(onClickListener: RecyclerOnClickListenerInterface?) {
        this.onClickListener = onClickListener
    }

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
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
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight)
        val onTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (onClickListener != null && v is IGraph) {
                    onClickListener!!.onClick(v, (v as IGraph).getItemPositionFromPoint(event.x))
                }
                return@OnTouchListener true
            }
            false
        }
        barChartView.layoutParams = layoutParams
        barChartView.setOnTouchListener(onTouchListener)
        barChartView.setDrawDataLabels(true)
        barChartView.setDrawIconLabels(true)
        barChartView.setCenterGraphView(true)

        removeAllViews()
        this.addView(barChartView)

        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true)

        resetView()
    }

    private fun updateViewColors() {
        val systemNightMode = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES
        val bottomTextColor = if (isNightMode) Colors.WHITE else Colors.BLACK

        barChartView.setBottomTextColor(bottomTextColor)
    }

    private fun resetView() {
        updateViewColors()

        barChartView.resetData(false)

        updateGraphData()

        barChartView.smoothScrollTo(0, 0)
    }

    private fun updateGraphData() {
        barChartView.data = graphData
    }

    fun setGraphData(data: RangeBarGraphData?) {
        this.graphData = data
        barChartView.data = data
        resetView()
    }
}