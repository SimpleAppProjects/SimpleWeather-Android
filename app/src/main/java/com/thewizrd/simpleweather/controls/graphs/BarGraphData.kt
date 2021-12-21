package com.thewizrd.simpleweather.controls.graphs

class BarGraphData : GraphData<BarGraphDataSet> {
    constructor() : super()
    constructor(set: BarGraphDataSet) : super(set)

    fun setDataSet(set: BarGraphDataSet) {
        dataSets.clear()
        dataSets.add(set)
        notifyDataChanged()
    }

    fun getDataSet(): BarGraphDataSet? {
        return dataSets.firstOrNull()
    }
}