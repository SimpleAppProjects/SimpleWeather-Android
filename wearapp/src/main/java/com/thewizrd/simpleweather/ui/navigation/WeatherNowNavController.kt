package com.thewizrd.simpleweather.ui.navigation

import android.os.Bundle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WeatherNowNavController {
    private var _currentDestinationFlow: MutableSharedFlow<Destination?> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    val currentDestinationFlow: Flow<Destination?> =
        _currentDestinationFlow.asSharedFlow()

    fun navigate(route: String?) {
        _currentDestinationFlow.tryEmit(route?.let { Destination(it) })
    }

    fun navigate(route: String?, args: Bundle) {
        _currentDestinationFlow.tryEmit(route?.let { Destination(it, args) })
    }

    inner class Destination {
        var route: String? = null
        var args: Bundle = Bundle.EMPTY

        internal constructor(route: String?) {
            this.route = route
        }

        internal constructor(route: String?, args: Bundle) {
            this.route = route
            this.args = args
        }
    }
}