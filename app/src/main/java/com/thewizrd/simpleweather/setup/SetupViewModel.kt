package com.thewizrd.simpleweather.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.thewizrd.shared_resources.locationdata.LocationData

class SetupViewModel(private val state: SavedStateHandle) : ViewModel() {
    var locationData: LocationData? = null
}