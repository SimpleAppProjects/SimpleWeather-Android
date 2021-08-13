package com.thewizrd.simpleweather.controls.graphs

abstract class GraphDataSet<T : GraphEntry> : IGraphData {
    var entryData: MutableList<T>
        protected set

    var yMax = -Float.MAX_VALUE
        protected set
    var yMin = Float.MAX_VALUE
        protected set

    constructor() {
        this.entryData = ArrayList()
    }

    constructor(entries: List<T>) {
        this.entryData = entries as? MutableList ?: entries.toMutableList() ?: ArrayList()
        calcMinMax()
    }

    fun calcMinMax() {
        yMax = -Float.MAX_VALUE
        yMin = Float.MAX_VALUE

        if (entryData.isNullOrEmpty()) {
            return
        }

        for (entry in entryData) {
            calcMinMax(entry)
        }
    }

    protected abstract fun calcMinMax(entry: T)

    fun setEntries(entries: List<T>) {
        this.entryData = entries as? MutableList ?: entries.toMutableList() ?: ArrayList()
        notifyDataSetChanged()
    }

    override fun getDataCount(): Int {
        return entryData.size
    }

    override fun isEmpty(): Boolean {
        return entryData.isEmpty()
    }

    fun clear() {
        entryData.clear()
        notifyDataSetChanged()
    }

    fun addEntry(entry: T): Boolean {
        calcMinMax(entry)
        return entryData.add(entry)
    }

    fun removeEntry(entry: T): Boolean {
        val removed = entryData.remove(entry)

        if (removed) {
            calcMinMax()
        }

        return removed
    }

    fun getEntryIndex(entry: GraphEntry): Int {
        return entryData.indexOf(entry)
    }

    fun getEntryForIndex(idx: Int): T {
        return entryData[idx]
    }

    fun notifyDataSetChanged() {
        calcMinMax()
    }
}
