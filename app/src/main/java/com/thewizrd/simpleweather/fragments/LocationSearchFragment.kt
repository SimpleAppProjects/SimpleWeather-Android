package com.thewizrd.simpleweather.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialContainerTransform
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding
import com.thewizrd.simpleweather.databinding.SearchActionBarBinding
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback
import kotlinx.coroutines.*

class LocationSearchFragment : WindowColorFragment() {
    companion object {
        private const val TAG = "LocSearchFragment"
        private const val KEY_SEARCHTEXT = "search_text"
    }

    private lateinit var binding: FragmentLocationSearchBinding
    private lateinit var searchBarBinding: SearchActionBarBinding
    private lateinit var mAdapter: ConcatAdapter
    private lateinit var mLocationAdapter: LocationQueryAdapter
    private lateinit var mFooterAdapter: LocationQueryFooterAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private val locationSearchViewModel: LocationSearchViewModel by navGraphViewModels("/locations")

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initSnackManager(context as Activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = Constants.ANIMATION_DURATION.toLong()
        }
    }

    override fun onPause() {
        searchBarBinding.searchView.clearFocus()
        super.onPause()
    }

    override fun onDetach() {
        unloadSnackManager()
        super.onDetach()
    }

    override fun createSnackManager(activity: Activity): SnackbarManager {
        return SnackbarManager(activity.findViewById(android.R.id.content)).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }

    private val recyclerClickListener = object : ListAdapterOnClickInterface<LocationQuery> {
        override fun onClick(view: View, item: LocationQuery) {
            locationSearchViewModel.onLocationSelected(item)
        }
    }

    private fun showLoading(show: Boolean) {
        searchBarBinding.searchProgressBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show || searchBarBinding.searchView.text.isNullOrBlank())
            searchBarBinding.searchCloseButton.visibility = View.GONE
        else
            searchBarBinding.searchCloseButton.visibility = View.VISIBLE
    }

    private fun enableRecyclerView(enable: Boolean) {
        binding.recyclerView.isEnabled = enable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        searchBarBinding = binding.searchBar
        searchBarBinding.lifecycleOwner = viewLifecycleOwner

        ViewCompat.setTransitionName(binding.root, Constants.SHARED_ELEMENT)
        ViewGroupCompat.setTransitionGroup((binding.root as ViewGroup), true)

        // Initialize
        searchBarBinding.searchBackButton.setOnClickListener { v ->
            v.findNavController().navigateUp()
        }
        searchBarBinding.searchCloseButton.setOnClickListener {
            searchBarBinding.searchView.setText("")
        }
        searchBarBinding.searchCloseButton.visibility = View.GONE

        searchBarBinding.searchView.addTextChangedListener(object : TextWatcher {
            private var textChangedJob: Job? = null

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // nothing to do here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // user is typing: reset already started timer (if existing)
                textChangedJob?.cancel()
            }

            override fun afterTextChanged(e: Editable) {
                // If string is null or empty (ex. from clearing text) run right away
                if (e.isBlank()) {
                    runSearchOp(e)
                } else {
                    textChangedJob = runWithView {
                        supervisorScope {
                            delay(1000)
                            ensureActive()
                            runSearchOp(e)
                        }
                    }
                }
            }

            private fun runSearchOp(e: Editable) {
                val newText = e.toString()
                searchBarBinding.searchCloseButton.isVisible = newText.isNotEmpty()
                fetchLocations(newText)
            }
        })
        searchBarBinding.searchView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showInputMethod(v.findFocus())
            } else {
                hideInputMethod(v)
            }
        }
        searchBarBinding.searchView.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                fetchLocations(v.text.toString())
                hideInputMethod(v)
                return@OnEditorActionListener true
            }
            false
        })

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, i ->
            val insets = i.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }

            i
        }

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = mLayoutManager

        // specify an adapter (see also next example)
        mLocationAdapter = LocationQueryAdapter()
        mLocationAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = ConcatAdapter(mLocationAdapter).also { mAdapter = it }
        mFooterAdapter = LocationQueryFooterAdapter()

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!text.isNullOrBlank()) {
                searchBarBinding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestSearchbarFocus()

        val navController = findNavController()

        /*
         * Capture touch events on RecyclerView
         * We're not using ADJUST_RESIZE so hide the keyboard when necessary
         * Hide the keyboard if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
         * Leave the keyboard up if we're scrolling to the top (items at the top are already visible)
         */
        val gestureDetector = GestureDetectorCompat(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                private val v = binding.recyclerView
                private var mY = 0
                private var shouldCloseKeyboard = false

                override fun onDown(e: MotionEvent): Boolean {
                    mY = e.y.toInt()
                    return super.onDown(e)
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    navController.navigateUp()
                    return super.onSingleTapConfirmed(e)
                }

                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    val newY = e2.y.toInt()
                    val dY = mY - newY
                    mY = newY
                    // Set flag to hide the keyboard if we're scrolling down
                    // So we can see what's behind the keyboard
                    shouldCloseKeyboard = dY > 0

                    if (shouldCloseKeyboard) {
                        hideInputMethod(v)
                        shouldCloseKeyboard = false
                    }

                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val newY = e2.y.toInt()
                    val dY = mY - newY
                    mY = newY
                    // Set flag to hide the keyboard if we're scrolling down
                    // So we can see what's behind the keyboard
                    shouldCloseKeyboard = dY > 0

                    if (shouldCloseKeyboard) {
                        hideInputMethod(v)
                        shouldCloseKeyboard = false
                    }

                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        binding.recyclerView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.isLoading.collect { loading ->
                showLoading(loading)
                enableRecyclerView(!loading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.locations.collect {
                if (it.isNotEmpty()) {
                    mLocationAdapter.submitList(it)
                    mAdapter.addAdapter(mFooterAdapter)
                } else {
                    mLocationAdapter.submitList(it)
                    mAdapter.removeAdapter(mFooterAdapter)
                }
            }
        }
    }

    override fun updateWindowColors() {
        context?.let { ctx ->
            var backgroundColor = ctx.getAttrColor(android.R.attr.colorBackground)
            var statusBarColor = ctx.getAttrColor(R.attr.colorSurface)
            if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                backgroundColor = Colors.BLACK
                statusBarColor = Colors.BLACK
            }

            binding.rootView.setBackgroundColor(backgroundColor)
            if (binding.appBar.background is MaterialShapeDrawable) {
                val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                binding.appBar.setBackgroundColor(statusBarColor)
            }
        }
    }

    override fun onDestroyView() {
        hideInputMethod(searchBarBinding.searchView)
        super.onDestroyView()
    }

    fun fetchLocations(queryString: String?) {
        locationSearchViewModel.fetchLocations(queryString)
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                activity?.let {
                    showSnackbar(
                        Snackbar.make(it, error.stringId, Snackbar.Duration.SHORT),
                        SnackbarWindowAdjustCallback(it)
                    )
                }
            }
            is ErrorMessage.String -> {
                activity?.let {
                    showSnackbar(
                        Snackbar.make(it, error.message, Snackbar.Duration.SHORT),
                        SnackbarWindowAdjustCallback(it)
                    )
                }
            }
            is ErrorMessage.WeatherError -> {
                activity?.let {
                    showSnackbar(
                        Snackbar.make(it, error.exception.message, Snackbar.Duration.SHORT),
                        SnackbarWindowAdjustCallback(it)
                    )
                }
            }
        }

        locationSearchViewModel.setErrorMessageShown(error)
    }

    private fun requestSearchbarFocus() {
        searchBarBinding.searchView.requestFocus()
    }

    private fun showInputMethod(view: View?) {
        view?.let {
            val imm =
                it.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.showSoftInput(it, 0)
        }
    }

    private fun hideInputMethod(view: View?) {
        view?.let {
            val imm =
                it.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SEARCHTEXT,
                if (!searchBarBinding.searchView.text.isNullOrBlank()) {
                    searchBarBinding.searchView.text.toString()
                } else {
                    ""
                })

        super.onSaveInstanceState(outState)
    }
}