package com.thewizrd.simpleweather.controls.graphs

class RangeBarGraphDataSet(entries: List<RangeBarGraphEntry>) :
    GraphDataSet<RangeBarGraphEntry>(entries) {

    override fun calcMinMax(entry: RangeBarGraphEntry) {
        if (entry.hiTempData?.y != null && entry.hiTempData!!.y < yMin) {
            yMin = entry.hiTempData!!.y
        }

        if (entry.hiTempData?.y != null && entry.hiTempData!!.y > yMax) {
            yMax = entry.hiTempData!!.y
        }

        if (entry.loTempData?.y != null && entry.loTempData!!.y < yMin) {
            yMin = entry.loTempData!!.y
        }

        if (entry.loTempData?.y != null && entry.loTempData!!.y > yMax) {
            yMax = entry.loTempData!!.y
        }
    }
}