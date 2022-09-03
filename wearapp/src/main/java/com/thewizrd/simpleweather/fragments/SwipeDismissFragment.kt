package com.thewizrd.simpleweather.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.wear.widget.SwipeDismissFrameLayout
import com.thewizrd.simpleweather.databinding.LayoutSwipedismissableBinding

open class SwipeDismissFragment : CustomFragment() {
    private lateinit var binding: LayoutSwipedismissableBinding
    private var swipeCallback: SwipeDismissFrameLayout.Callback? = null

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LayoutSwipedismissableBinding.inflate(inflater, container, false)

        binding.swipeLayout.isSwipeable = true
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
        binding.swipeLayout.addCallback(swipeCallback)

        return binding.swipeLayout
    }

    override fun onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }
}