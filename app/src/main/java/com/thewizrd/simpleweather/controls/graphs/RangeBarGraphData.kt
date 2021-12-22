package com.thewizrd.simpleweather.controls.graphs

class RangeBarGraphData : GraphData<RangeBarGraphDataSet> {
    constructor() : super()
    constructor(set: RangeBarGraphDataSet) : super(set)

    fun setDataSet(set: RangeBarGraphDataSet) {
        dataSets.clear()
        dataSets.add(set)
        notifyDataChanged()
    }

    fun getDataSet(): RangeBarGraphDataSet? {
        return if (dataSets.size != 0) dataSets.firstOrNull() else null
    }
}