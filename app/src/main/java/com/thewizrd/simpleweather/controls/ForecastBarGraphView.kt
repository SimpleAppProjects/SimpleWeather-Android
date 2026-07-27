package com.thewizrd.simpleweather.controls

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.BarGraphData
import com.thewizrd.simpleweather.controls.graphs.BarGraphEntry
import com.thewizrd.simpleweather.controls.viewmodels.ForecastType
import com.thewizrd.simpleweather.databinding.LayoutBarBinding
import com.thewizrd.simpleweather.databinding.LayoutBarViewBinding
import kotlin.math.max
import com.thewizrd.shared_resources.R as sharedRes

class ForecastBarGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val binding: LayoutBarViewBinding

    private val graphHeight: Int
    private val zeroValueItemHeight: Int
    private var bottomTextHeights: Int = 0

    private var graphData: BarGraphData? = null
    private var forecastType: ForecastType? = null

    // Event listeners
    private var onClickListener: RecyclerOnClickListenerInterface? = null

    fun setOnClickPositionListener(onClickListener: RecyclerOnClickListenerInterface?) {
        this.onClickListener = onClickListener
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.innerLayout.setOnClickListener(l)
    }

    init {
        orientation = VERTICAL
        zeroValueItemHeight = context.dpToPx(1f).toInt()

        val inflater = LayoutInflater.from(context)
        binding = LayoutBarViewBinding.inflate(inflater, this)

        graphHeight = context.obtainStyledAttributes(attrs, R.styleable.ForecastBarGraphView).use {
            it.getDimensionPixelSize(
                R.styleable.ForecastBarGraphView_graphHeight,
                context.resources.getDimensionPixelSize(R.dimen.bargraph_panel_height)
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val barIconHeight = context.dpToPx(32f).toInt()
        var heightChanged = false

        binding.innerLayout.forEachIndexed { idx, bar ->
            val barBinding = DataBindingUtil.getBinding<LayoutBarBinding>(bar)
            if (barBinding != null) {
                if (idx == 0) {
                    val measuredBottomTextHeights =
                        barBinding.barValue.measuredHeight + barBinding.barDate.measuredHeight
                    if (bottomTextHeights != measuredBottomTextHeights) {
                        bottomTextHeights = measuredBottomTextHeights
                    }
                }

                // Update bar measurement
                graphData?.getDataSet()?.getEntryForIndex(idx)?.let { data ->
                    val max = graphData?.yMax
                    val min = graphData?.yMin
                    if (min != null && max != null) {
                        if (updateInnerBarHeight(barBinding.innerBar, min, max, data)) {
                            heightChanged = true
                        }
                    }
                }

                if (!heightChanged) {
                    val shouldBeGone = (barBinding.innerBar.measuredHeight.takeIf { it > 0 }
                        ?: barBinding.innerBar.layoutParams.height) <= barIconHeight

                    if (shouldBeGone != barBinding.barIcon.isGone) {
                        barBinding.barIcon.isGone = shouldBeGone
                    }
                }
            }
        }

        if (heightChanged) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            binding.innerLayout.forEach { bar ->
                val barBinding = DataBindingUtil.getBinding<LayoutBarBinding>(bar)
                if (barBinding != null) {
                    val shouldBeGone = (barBinding.innerBar.measuredHeight.takeIf { it > 0 }
                        ?: barBinding.innerBar.layoutParams.height) <= barIconHeight

                    if (shouldBeGone != barBinding.barIcon.isGone) {
                        barBinding.barIcon.isGone = shouldBeGone
                        barBinding.innerBar.forceLayout()
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    fun setData(graphData: BarGraphData?, forecastType: ForecastType? = null) {
        this.graphData = graphData
        this.forecastType = forecastType

        val dataSet = graphData?.getDataSet()

        if (dataSet != null && !dataSet.isEmpty) {
            binding.innerLayout.run {
                val itemCount = dataSet.dataCount
                val layoutInflater = LayoutInflater.from(context)

                val max = graphData.yMax
                val min = graphData.yMin

                for (i in 0 until itemCount) {
                    val view = getChildAt(i)
                    val data = dataSet.getEntryForIndex(i)

                    val item: LayoutBarBinding = if (view != null) {
                        DataBindingUtil.getBinding(view) ?: LayoutBarBinding.bind(view)
                    } else {
                        LayoutBarBinding.inflate(layoutInflater)
                    }

                    item.data = data
                    item.root.setOnClickListener { v ->
                        onClickListener?.onClick(v, max(indexOfChild(v), 0))
                    }

                    updateInnerBarHeight(item.innerBar, min, max, data)

                    // Update icon
                    item.barIcon.rotation = data.xIconRotation.toFloat()
                    item.barIcon.setImageResource(
                        getIconResourceFromWeatherIcon(data.xWeatherIcon)
                            ?: getIconResourceFromForecastType(forecastType)
                    )

                    if (getChildAt(i) == null) {
                        addView(
                            item.root,
                            LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                                .apply {
                                    gravity = Gravity.BOTTOM
                                }
                        )
                    }
                }

                removeViews(itemCount, childCount - itemCount)
                this@ForecastBarGraphView.requestLayout()
            }
        } else {
            binding.innerLayout.removeAllViews()
        }
    }

    private fun updateInnerBarHeight(
        innerBar: View,
        min: Float,
        max: Float,
        data: BarGraphEntry
    ): Boolean {
        var heightChanged = false

        innerBar.updateLayoutParams {
            val normalizedValue = when {
                min == max -> {
                    1f
                }

                else -> {
                    val dataRange = max - min

                    data.entryData?.y?.let {
                        if (dataRange == 0f) {
                            0f
                        } else {
                            (it.coerceIn(min, max) - min) / dataRange
                        }
                    } ?: 0f
                }
            }
            val newHeight =
                zeroValueItemHeight + (normalizedValue * (graphHeight - bottomTextHeights)).toInt()

            if (height != newHeight) {
                height = newHeight
                heightChanged = true
            }
        }

        return heightChanged
    }

    @DrawableRes
    private fun getIconResourceFromWeatherIcon(icon: String?): Int? {
        return when (icon) {
            WeatherIcons.NA, null -> null
            else -> sharedDeps.weatherIconsManager.iconProvider.getWeatherIconResource(icon)
        }
    }

    @DrawableRes
    private fun getIconResourceFromForecastType(forecastType: ForecastType?): Int {
        return when (forecastType) {
            ForecastType.TEMPERATURE -> 0
            ForecastType.MINUTELY -> sharedRes.drawable.wi_raindrop
            ForecastType.PRECIPITATION -> sharedRes.drawable.wi_raindrop
            ForecastType.WIND -> sharedRes.drawable.wi_direction_up_2x
            ForecastType.HUMIDITY -> sharedRes.drawable.material_humidity_percentage
            ForecastType.UVINDEX -> 0
            ForecastType.RAIN -> sharedRes.drawable.material_water_drop
            ForecastType.SNOW -> sharedRes.drawable.wi_snowflake_cold
            ForecastType.AIRQUALITY -> 0
            null -> 0
        }
    }
}