package com.thewizrd.simpleweather.controls.graphs

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class BarGraphEntry : GraphEntry {
    var entryData: YEntryData? = null

    @ColorInt
    var fillColor: Int? = null

    constructor()

    @JvmOverloads
    constructor(
            label: CharSequence,
            entryData: YEntryData,
            icon: Drawable? = null
    ) {
        this.xLabel = label
        this.entryData = entryData
        this.xIcon = icon
    }
}