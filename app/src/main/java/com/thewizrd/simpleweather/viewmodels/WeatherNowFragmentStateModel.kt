package com.thewizrd.simpleweather.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class WeatherNowFragmentStateModel(private val state: SavedStateHandle) : ViewModel() {
    var scrollViewPosition: Int
        get() {
            return state["scrollViewPosition"] ?: 0
        }
        set(value) {
            state["scrollViewPosition"] = value
        }
}