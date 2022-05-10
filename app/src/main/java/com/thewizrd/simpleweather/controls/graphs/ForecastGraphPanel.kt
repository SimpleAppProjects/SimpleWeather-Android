package com.thewizrd.simpleweather.controls.graphs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.LinearLayout
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewGroupCompat
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.R

class ForecastGraphPanel : LinearLayout, GraphPanel {
    private lateinit var lineView: LineView

    private var graphData: LineViewData? = null

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

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

        lineView.postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize(context: Context) {
        currentConfig = Configuration(context.resources.configuration)
        orientation = VERTICAL
        lineView = LineView(context)

        val lineViewHeight = context.resources.getDimensionPixelSize(R.dimen.forecast_panel_height)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight).apply {
            gravity = Gravity.CENTER
        }
        val onTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (onClickListener != null && v is IGraph) {
                    onClickListener!!.onClick(v, (v as IGraph).getItemPositionFromPoint(event.x))
                }
                return@OnTouchListener true
            }
            false
        }
        lineView.layoutParams = layoutParams
        lineView.setOnTouchListener(onTouchListener)
        lineView.setDrawGridLines(false)
        lineView.setDrawDotLine(false)
        lineView.setDrawDataLabels(true)
        lineView.setDrawIconLabels(true)
        lineView.setDrawGraphBackground(true)
        lineView.setDrawDotPoints(false)
        lineView.setFillParentWidth(true)

        removeAllViews()
        this.addView(lineView)

        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true)

        resetView()
    }

    private fun updateViewColors() {
        val systemNightMode = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES
        val bottomTextColor = if (isNightMode) Colors.WHITE else Colors.BLACK

        lineView.setBackgroundLineColor(
            ColorUtils.setAlphaComponent(
                if (isNightMode) Colors.WHITE else Colors.GRAY,
                0x99
            )
        )
        lineView.setBottomTextColor(bottomTextColor)
    }

    private fun resetView() {
        updateViewColors()

        lineView.resetData(false)

        updateGraphData()

        lineView.smoothScrollTo(0, 0)
    }

    private fun updateGraphData() {
        lineView.data = graphData
    }

    fun setGraphData(data: LineViewData?) {
        this.graphData = data
        lineView.data = data
        resetView()
    }

    override fun setDrawIconLabels(drawIconsLabels: Boolean) {
        lineView.setDrawIconLabels(drawIconsLabels)
    }

    override fun setDrawDataLabels(drawDataLabels: Boolean) {
        lineView.setDrawDataLabels(drawDataLabels)
    }

    override fun setScrollingEnabled(enabled: Boolean) {
        lineView.isScrollingEnabled = enabled
    }

    override fun getItemPositionFromPoint(xCoordinate: Float): Int {
        return lineView.getItemPositionFromPoint(xCoordinate)
    }

    override fun setBottomTextSize(@Px textSize: Float) {
        lineView.setBottomTextSize(textSize)
    }

    override fun setIconSize(iconSize: Float) {
        lineView.setIconSize(iconSize)
    }

    override fun setGraphMaxWidth(maxWidth: Int) {
        lineView.setGraphMaxWidth(maxWidth)
    }

    override fun setFillParentWidth(fillParentWidth: Boolean) {
        lineView.setFillParentWidth(fillParentWidth)
    }

    override fun setBottomTextColor(color: Int) {
        lineView.setBottomTextColor(color)
    }

    override fun requestGraphLayout() {
        lineView.requestGraphLayout()
    }
}