package com.thewizrd.simpleweather.controls.graphs

import android.graphics.drawable.Drawable

class LineGraphEntry : GraphEntry {
    var yEntryData: YEntryData

    @JvmOverloads
    constructor(label: CharSequence, yEntryData: YEntryData, icon: Drawable? = null) {
        this.xLabel = label
        this.yEntryData = yEntryData
        this.xIcon = icon
    }
}