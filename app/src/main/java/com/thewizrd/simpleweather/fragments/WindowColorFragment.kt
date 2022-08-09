package com.thewizrd.simpleweather.fragments

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thewizrd.simpleweather.helpers.WindowColorManager
import kotlinx.coroutines.launch

abstract class WindowColorFragment : CustomFragment(), WindowColorManager {
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateWindowColors()
            }
        }
    }

    abstract override fun updateWindowColors()
}