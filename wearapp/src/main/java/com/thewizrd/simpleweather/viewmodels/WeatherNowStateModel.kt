package com.thewizrd.simpleweather.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class WeatherNowStateModel(private val state: SavedStateHandle) : ViewModel() {
    private val loadingState = MutableStateFlow(true)

    val isLoading = loadingState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        loadingState.value
    )

    var scrollViewPosition: Int
        get() {
            return state["scrollViewPosition"] ?: 0
        }
        set(value) {
            state["scrollViewPosition"] = value
        }

    fun updateLoadingState(loading: Boolean) {
        loadingState.update { loading }
    }
}