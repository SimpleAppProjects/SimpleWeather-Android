package com.thewizrd.simpleweather.controls.graphs

import androidx.annotation.ColorInt
import androidx.annotation.Px

interface GraphPanel : IGraph {
    fun setGraphMaxWidth(@Px maxWidth: Int)
    fun setFillParentWidth(fillParentWidth: Boolean)
    fun setBottomTextColor(@ColorInt color: Int)
    fun setBottomTextSize(@Px textSize: Float)
    fun setIconSize(@Px iconSize: Float)
    fun setDrawIconLabels(drawIconsLabels: Boolean)
    fun setDrawDataLabels(drawDataLabels: Boolean)
    fun setScrollingEnabled(enabled: Boolean)
    fun requestGraphLayout()
}