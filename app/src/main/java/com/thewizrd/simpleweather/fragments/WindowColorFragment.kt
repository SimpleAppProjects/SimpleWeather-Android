package com.thewizrd.simpleweather.fragments

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.thewizrd.simpleweather.helpers.WindowColorManager

abstract class WindowColorFragment : CustomFragment(), WindowColorManager {
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                updateWindowColors()
            }
        })
    }

    abstract override fun updateWindowColors()
}