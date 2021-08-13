package com.thewizrd.simpleweather.controls.graphs

import android.graphics.drawable.Drawable

abstract class GraphEntry {
    var xLabel: CharSequence? = null
    var xIcon: Drawable? = null
    var xIconRotation: Int = 0
}
