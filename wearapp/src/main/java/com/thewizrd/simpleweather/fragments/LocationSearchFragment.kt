package com.thewizrd.simpleweather.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.common.helpers.SpacerItemDecoration
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ListHeaderAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding
import com.thewizrd.simpleweather.helpers.CustomScrollingLayoutCallback
import kotlinx.coroutines.launch

class LocationSearchFragment : SwipeDismissFragment() {
    companion object {
        private const val TAG = "LocSearchFragment"
        private const val KEY_SEARCHTEXT = "search_text"
        private const val REQUEST_CODE_VOICE_INPUT = 0
    }

    private lateinit var binding: FragmentLocationSearchBinding
    private lateinit var mAdapter: LocationQueryAdapter
    private lateinit var swipeCallback: SwipeDismissFrameLayout.Callback

    private val locationSearchViewModel: LocationSearchViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")
    }

    private val recyclerClickListener = object : ListAdapterOnClickInterface<LocationQuery> {
        override fun onClick(view: View, item: LocationQuery) {
            if (item != LocationQuery.EMPTY) {
                locationSearchViewModel.onLocationSelected(item)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarContainer.isVisible = show
        if (show) {
            binding.recyclerViewLayout.visibility = View.GONE
            binding.recyclerViewLayout.clearFocus()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, view, true)
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                layout.visibility = View.GONE
            }
        }
        binding.recyclerViewLayout.addCallback(swipeCallback)
        binding.keyboardButton.setOnClickListener {
            binding.searchView.requestFocus()
            showInputMethod(binding.searchView)
        }
        binding.voiceButton.setOnClickListener {
            binding.searchView.setText("")
            view!!.requestFocus()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    it.context.getString(R.string.location_search_hint)
                )
            }

            startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT)
        }
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // If we're using searchfragment
                // make sure gps feature is off
                if (settingsManager.useFollowGPS()) {
                    settingsManager.setFollowGPS(false)
                }
            }
        })
        binding.searchView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showInputMethod(v)
            } else {
                hideInputMethod(v)
            }
        }
        binding.searchView.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearchAction()

                // If we're using searchfragment
                // make sure gps feature is off
                if (settingsManager.useFollowGPS()) {
                    settingsManager.setFollowGPS(false)
                }
                return@OnEditorActionListener true
            }
            false
        })

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        binding.recyclerView.addItemDecoration(
            SpacerItemDecoration(
                requireContext().dpToPx(16f).toInt(),
                requireContext().dpToPx(4f).toInt()
            )
        )
        binding.recyclerView.layoutManager =
            WearableLinearLayoutManager(requireContext(), CustomScrollingLayoutCallback())

        // specify an adapter (see also next example)
        mAdapter = LocationQueryAdapter()
        mAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = ConcatAdapter(
            ListHeaderAdapter(getString(R.string.label_nav_locations)),
            mAdapter,
            LocationQueryFooterAdapter(),
            SpacerAdapter(requireContext().dpToPx(48f).toInt())
        )
        binding.recyclerView.requestFocus()

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!text.isNullOrEmpty()) {
                binding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.isLoading.collect { loading ->
                showLoading(loading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.locations.collect {
                mAdapter.submitList(it)

                if (it.isNotEmpty()) {
                    binding.recyclerViewLayout.isVisible = true
                    binding.recyclerViewLayout.requestFocus()
                } else {
                    binding.recyclerViewLayout.isVisible = false
                    binding.recyclerViewLayout.clearFocus()
                }
            }
        }
    }

    override fun onDestroyView() {
        hideInputMethod(binding.searchView)
        binding.recyclerViewLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }

    private fun doSearchAction() {
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerViewLayout.visibility = View.VISIBLE
        binding.recyclerViewLayout.requestFocus()

        hideInputMethod(binding.searchView)

        fetchLocations(binding.searchView.text.toString())
    }

    private fun fetchLocations(queryString: String?) {
        locationSearchViewModel.fetchLocations(queryString)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_VOICE_INPUT -> {
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (!results.isNullOrEmpty()) {
                    val text = results[0]
                    if (!text.isNullOrEmpty()) {
                        binding.searchView.setText(text)
                        doSearchAction()
                    }
                }
            }
        }
    }

    private fun showInputMethod(view: View?) {
        view?.let {
            val imm =
                it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.showSoftInput(it, InputMethodManager.SHOW_FORCED)
        }
    }

    private fun hideInputMethod(view: View?) {
        view?.let {
            val imm =
                it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SEARCHTEXT,
                if (!binding.searchView.text.isNullOrEmpty()) {
                    binding.searchView.text.toString()
                } else {
                    ""
                })

        super.onSaveInstanceState(outState)
    }
}