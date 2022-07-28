package com.thewizrd.simpleweather.ui

import androidx.compose.foundation.ScrollState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.wear.compose.material.ScalingLazyListState

const val SCROLL_STATE_KEY = "scrollState"

// Saves scroll state through process death; inspired by https://issuetracker.google.com/195689777
class ScalingLazyListStateViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    @OptIn(SavedStateHandleSaveableApi::class)
    val scrollState = savedStateHandle.saveable(
        key = SCROLL_STATE_KEY,
        saver = ScalingLazyListState.Saver
    ) {
        ScalingLazyListState()
    }
}

class ScrollStateViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    @OptIn(SavedStateHandleSaveableApi::class)
    val scrollState = savedStateHandle.saveable(
        key = SCROLL_STATE_KEY,
        saver = ScrollState.Saver
    ) {
        ScrollState(0)
    }
}