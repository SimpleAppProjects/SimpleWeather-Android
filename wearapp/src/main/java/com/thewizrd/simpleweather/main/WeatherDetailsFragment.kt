package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.common.BR
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.adapters.DetailItemAdapter
import com.thewizrd.simpleweather.adapters.DetailItemFooterAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment
import com.thewizrd.simpleweather.helpers.CustomScrollingLayoutCallback

class WeatherDetailsFragment : SwipeDismissFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var mAdapter: DetailItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherDetails: onCreate")

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                mAdapter.submitList(weatherView.weatherDetailsMap.values.toList())
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use this to return your custom view for this Fragment
        val outerView = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        binding = FragmentWeatherListBinding.inflate(inflater, outerView)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager =
            WearableLinearLayoutManager(requireContext(), CustomScrollingLayoutCallback())
        binding.recyclerView.requestFocus()

        mAdapter = DetailItemAdapter()

        binding.recyclerView.adapter = ConcatAdapter(
            SpacerAdapter(requireContext().dpToPx(48f).toInt()),
            mAdapter,
            DetailItemFooterAdapter(),
            SpacerAdapter(requireContext().dpToPx(48f).toInt())
        )
        binding.recyclerView.addItemDecoration(
            SpacerItemDecoration(
                requireContext().dpToPx(16f).toInt(),
                requireContext().dpToPx(4f).toInt()
            )
        )

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                binding.timeText.apply {
                    translationY = -recyclerView.computeVerticalScrollOffset().toFloat()
                }
            }
        })

        return outerView
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherDetails: onResume")
        weatherView.addOnPropertyChangedCallback(propertyChangedCallback)
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherDetails: onPause")
        weatherView.removeOnPropertyChangedCallback(propertyChangedCallback)
        super.onPause()
    }

    private val propertyChangedCallback = object : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            runWithView {
                if (propertyId == BR.weatherDetailsMap) {
                    mAdapter.submitList(weatherView.weatherDetailsMap.values.toList())
                }
            }
        }
    }
}