package com.thewizrd.simpleweather.controls.graphs

import androidx.annotation.ColorInt
import com.thewizrd.shared_resources.utils.Colors

class LineDataSeries : GraphDataSet<LineGraphEntry> {
    companion object {
        private val DEFAULT_COLORS =
            listOf(Colors.SIMPLEBLUE, Colors.LIGHTSEAGREEN, Colors.YELLOWGREEN)
    }

    var seriesLabel: String? = null
    var seriesColors: List<Int>

    var seriesMin: Float? = null
    var seriesMax: Float? = null

    constructor(seriesData: List<LineGraphEntry>) : super(seriesData) {
        this.seriesLabel = null
        this.seriesColors = DEFAULT_COLORS
    }

    constructor(seriesLabel: String?, seriesData: List<LineGraphEntry>) : this(seriesData) {
        this.seriesLabel = seriesLabel
    }

    @ColorInt
    fun getColor(idx: Int): Int {
        return seriesColors[idx % seriesColors.size]
    }

    fun setSeriesColors(@ColorInt vararg colors: Int) {
        this.seriesColors = colors.toList()
    }

    fun setSeriesMinMax(seriesMin: Float?, seriesMax: Float?) {
        this.seriesMin = seriesMin
        this.seriesMax = seriesMax
    }

    override fun calcMinMax(entry: LineGraphEntry) {
        if (seriesMin == null && seriesMax == null) {
            if (entry.yEntryData.y > yMax) {
                yMax = entry.yEntryData.y
            }
            if (entry.yEntryData.y < yMin) {
                yMin = entry.yEntryData.y
            }
        }
    }
}