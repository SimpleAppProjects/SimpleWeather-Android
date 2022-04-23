package com.thewizrd.simpleweather.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.core.view.LayoutInflaterCompat
import androidx.fragment.app.FragmentActivity

/**
 * Adds custom view inflater to inflate AppCompat views instead of inheriting AppCompatActivity
 */
open class AppCompatLiteActivity : FragmentActivity {
    companion object {
        private const val TAG = "AppCompatLiteActivity"
    }

    constructor() : super() {
        initDelegate()
    }

    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId) {
        initDelegate()
    }

    private fun initDelegate() {
        addOnContextAvailableListener { installViewFactory() }
    }

    private fun installViewFactory() {
        val layoutInflater = LayoutInflater.from(this)
        if (layoutInflater.factory == null) {
            LayoutInflaterCompat.setFactory2(layoutInflater, AppCompatLiteViewInflater())
        } else {
            if (layoutInflater.factory2 !is AppCompatLiteViewInflater) {
                Log.i(
                    TAG, "The Activity's LayoutInflater already has a Factory installed"
                            + " so we can not install AppCompat's"
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}