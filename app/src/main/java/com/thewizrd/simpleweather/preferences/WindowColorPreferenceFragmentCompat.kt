package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.thewizrd.common.helpers.OnBackPressedFragmentListener
import com.thewizrd.simpleweather.helpers.WindowColorManager

abstract class WindowColorPreferenceFragmentCompat : CustomPreferenceFragmentCompat(),
    OnBackPressedFragmentListener, WindowColorManager {
    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                updateWindowColors()
            }
        })
    }

    override fun updateWindowColors() {}
}