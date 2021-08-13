package com.thewizrd.simpleweather.controls.graphs

abstract class GraphData<T : GraphDataSet<*>> : IGraphData {
    var dataSets: MutableList<T>
        protected set

    var yMax = -Float.MAX_VALUE
        protected set
    var yMin = Float.MAX_VALUE
        protected set

    var graphLabel: CharSequence? = null

    constructor() {
        dataSets = ArrayList()
    }

    constructor(vararg sets: T) {
        dataSets = sets.toMutableList()
        notifyDataChanged()
    }

    constructor(sets: List<T>) {
        dataSets = sets as? MutableList<T> ?: sets.toMutableList()
        notifyDataChanged()
    }

    fun notifyDataChanged() {
        calcMinMax()
    }

    protected fun calcMinMax() {
        yMax = -Float.MAX_VALUE
        yMin = Float.MAX_VALUE

        if (dataSets.isNullOrEmpty()) {
            return
        }

        for (set in dataSets) {
            calcMinMax(set)
        }
    }

    override fun getDataCount(): Int {
        return dataSets.size
    }

    override fun isEmpty(): Boolean {
        return dataSets.isEmpty()
    }

    fun getDataSetByIndex(idx: Int): T? {
        if (dataSets.isNullOrEmpty() || idx >= dataSets.size) {
            return null
        }

        return dataSets[idx]
    }

    fun addDataSet(set: T) {
        calcMinMax(set)
        dataSets.add(set)
    }

    fun removeDataSet(set: T): Boolean {
        val removed = dataSets.remove(set)

        if (removed) {
            notifyDataChanged()
        }

        return removed
    }

    protected open fun calcMinMax(set: T) {
        if (yMax < set.yMax)
            yMax = set.yMax
        if (yMin > set.yMin)
            yMin = set.yMin
    }

    fun clear() {
        dataSets.clear()
        notifyDataChanged()
    }

    fun getMaxDataSetLabelCount(): Int {
        var count = 0

        for (set in dataSets) {
            if (set.dataCount > count) {
                count = set.dataCount
            }
        }

        return count
    }

    // Note: All sets share the same set of labels and icons for the x-axis
    fun getDataLabels(): List<GraphEntry> {
        val set = dataSets.firstOrNull()

        return set?.entryData ?: emptyList()
    }
}