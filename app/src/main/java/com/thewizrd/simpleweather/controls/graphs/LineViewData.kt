package com.thewizrd.simpleweather.controls.graphs

class LineViewData : GraphData<LineDataSeries> {
    constructor(sets: List<LineDataSeries>) : super(sets)
    constructor(label: CharSequence, sets: List<LineDataSeries>) : super(sets) {
        this.graphLabel = label
    }

    override fun calcMinMax(set: LineDataSeries) {
        if (set.seriesMin == null && set.seriesMax == null) {
            if (yMax < set.yMax) {
                yMax = set.yMax
            }
            if (yMin > set.yMin) {
                yMin = set.yMin
            }
        } else {
            if (set.seriesMax != null && yMax < set.seriesMax!!) {
                yMax = set.seriesMax!!
            }
            if (set.seriesMin != null && yMin > set.seriesMin!!) {
                yMin = set.seriesMin!!
            }
        }
    }
}