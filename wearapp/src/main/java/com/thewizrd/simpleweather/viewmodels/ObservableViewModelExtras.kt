package com.thewizrd.simpleweather.viewmodels

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.databinding.Observable

@Composable
fun <T> Observable.observeAsState(propertyId: Int, block: () -> T): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(block()) }

    DisposableEffect(this, lifecycleOwner) {
        val callback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propId: Int) {
                if (propertyId == propId) {
                    state.value = block()
                }
            }
        }
        addOnPropertyChangedCallback(callback)
        onDispose {
            removeOnPropertyChangedCallback(callback)
        }
    }

    return state
}