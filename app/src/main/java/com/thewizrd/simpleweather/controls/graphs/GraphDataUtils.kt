package com.thewizrd.simpleweather.controls.graphs

fun IGraphData?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty
}