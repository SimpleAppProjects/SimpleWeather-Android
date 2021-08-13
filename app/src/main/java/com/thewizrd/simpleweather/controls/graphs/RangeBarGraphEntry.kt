package com.thewizrd.simpleweather.controls.graphs

import android.graphics.drawable.Drawable

class RangeBarGraphEntry : GraphEntry {
    var hiTempData: YEntryData? = null
    var loTempData: YEntryData? = null

    constructor()

    constructor(
        label: CharSequence,
        hiTempData: YEntryData,
        loTempData: YEntryData,
        icon: Drawable? = null
    ) {
        this.xLabel = label
        this.hiTempData = hiTempData
        this.loTempData = loTempData
        this.xIcon = icon
    }
}