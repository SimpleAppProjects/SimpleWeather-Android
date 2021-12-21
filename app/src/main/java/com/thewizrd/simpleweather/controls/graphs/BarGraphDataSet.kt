package com.thewizrd.simpleweather.controls.graphs

class BarGraphDataSet(entries: List<BarGraphEntry>) :
        GraphDataSet<BarGraphEntry>(entries) {

    fun setMinMax(min: Float? = null, max: Float? = null) {
        if (min != null) {
            this.yMin = min
        }

        if (max != null) {
            this.yMax = max
        }
    }

    override fun calcMinMax(entry: BarGraphEntry) {
        if (entry.entryData?.y != null && entry.entryData!!.y < yMin) {
            yMin = entry.entryData!!.y
        }

        if (entry.entryData?.y != null && entry.entryData!!.y > yMax) {
            yMax = entry.entryData!!.y
        }
    }
}