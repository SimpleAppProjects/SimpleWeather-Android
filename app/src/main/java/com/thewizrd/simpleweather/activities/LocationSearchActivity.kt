package com.thewizrd.simpleweather.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.thewizrd.common.utils.ActivityUtils.setFullScreen
import com.thewizrd.common.utils.ActivityUtils.setTransparentWindow
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchResult
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.helpers.ListAdapterOnClickInterface
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.LocationQueryAdapter
import com.thewizrd.simpleweather.adapters.LocationQueryFooterAdapter
import com.thewizrd.simpleweather.databinding.ActivityLocationSearchBinding
import com.thewizrd.simpleweather.databinding.SearchActionBarBinding
import com.thewizrd.simpleweather.helpers.WindowColorManager
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

class LocationSearch : ActivityResultContract<Void?, LocationSearchResult>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(context, LocationSearchActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LocationSearchResult {
        return intent.let {
            val data = JSONParser.deserializer<LocationData>(it?.getStringExtra(Constants.KEY_DATA))

            if (data != null && resultCode == LocationSearchActivity.RESULT_SUCCESS) {
                LocationSearchResult.Success(data)
            } else if (data != null && resultCode == LocationSearchActivity.RESULT_ALREADY_EXISTS) {
                LocationSearchResult.AlreadyExists(data)
            } else {
                LocationSearchResult.Failed(null)
            }
        }
    }
}

class LocationSearchActivity : UserLocaleActivity(), UserThemeMode.OnThemeChangeListener,
    WindowColorManager {
    companion object {
        private const val TAG = "LocSearchFragment"
        private const val KEY_SEARCHTEXT = "search_text"

        const val RESULT_SUCCESS = Activity.RESULT_OK
        const val RESULT_CANCELED = Activity.RESULT_CANCELED
        const val RESULT_ALREADY_EXISTS = -2
    }

    private lateinit var binding: ActivityLocationSearchBinding
    private lateinit var searchBarBinding: SearchActionBarBinding
    private lateinit var mAdapter: ConcatAdapter
    private lateinit var mLocationAdapter: LocationQueryAdapter
    private lateinit var mFooterAdapter: LocationQueryFooterAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private val locationSearchViewModel: LocationSearchViewModel by viewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable Activity Transitions. Optionally enable Activity transitions in your
        // theme with <item name=”android:windowActivityTransitions”>true</item>.
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)

        // Set the transition name, which matches Activity A’s start view transition name, on
        // the root view.
        findViewById<View>(android.R.id.content).transitionName = Constants.SHARED_ELEMENT

        // Attach a callback used to receive the shared elements from Activity A to be
        // used by the container transform transition.
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        // Set this Activity’s enter and return transition to a MaterialContainerTransform
        window.sharedElementEnterTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 300L
        }
        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 250L
        }
        window.sharedElementExitTransition = MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            duration = 250L
        }

        super.onCreate(savedInstanceState)

        // Inflate the layout for this fragment
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.lifecycleOwner = this
        searchBarBinding = binding.searchBar
        searchBarBinding.lifecycleOwner = this

        ViewCompat.setTransitionName(binding.root, null)
        ViewGroupCompat.setTransitionGroup((binding.root as ViewGroup), true)

        // Initialize
        searchBarBinding.searchBackButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            onBackPressedDispatcher.onBackPressed()
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
                    textChangedJob = lifecycleScope.launch {
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
        searchBarBinding.searchView.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    showInputMethod(v.findFocus())
                } else {
                    hideInputMethod(v)
                }
            }
        searchBarBinding.searchView.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
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

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = mLayoutManager

        // specify an adapter (see also next example)
        mLocationAdapter = LocationQueryAdapter()
        mLocationAdapter.setOnClickListener(recyclerClickListener)
        binding.recyclerView.adapter = ConcatAdapter(mLocationAdapter).also { mAdapter = it }
        mFooterAdapter = LocationQueryFooterAdapter()

        /*
         * Capture touch events on RecyclerView
         * We're not using ADJUST_RESIZE so hide the keyboard when necessary
         * Hide the keyboard if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
         * Leave the keyboard up if we're scrolling to the top (items at the top are already visible)
         */
        val gestureDetector = GestureDetectorCompat(this, gestureListener)

        binding.recyclerView.setOnTouchListener { _, event ->
            runCatching {
                gestureDetector.onTouchEvent(event)
            }.onFailure {
                Timber.tag(TAG).w(it)
            }.getOrElse { false }
        }

        if (savedInstanceState != null) {
            val text = savedInstanceState.getString(KEY_SEARCHTEXT)
            if (!text.isNullOrBlank()) {
                searchBarBinding.searchView.setText(text, TextView.BufferType.EDITABLE)
            }
        }

        val color = getAttrColor(R.attr.colorPrimarySurface)
        window.setTransparentWindow(color)

        lifecycleScope.launch {
            locationSearchViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.isLoading.collect { loading ->
                showLoading(loading)
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.locations.collectLatest {
                if (it.isNotEmpty()) {
                    mLocationAdapter.submitList(it)
                    mAdapter.addAdapter(mFooterAdapter)
                } else {
                    mLocationAdapter.submitList(it)
                    mAdapter.removeAdapter(mFooterAdapter)
                }
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.selectedSearchLocation.collectLatest { result ->
                when (result) {
                    is LocationSearchResult.AlreadyExists -> {
                        setResult(RESULT_ALREADY_EXISTS, Intent().apply {
                            putExtra(Constants.KEY_DATA, JSONParser.serializer(result.data))
                        })
                        finish()
                    }
                    is LocationSearchResult.Success -> {
                        setResult(RESULT_SUCCESS, Intent().apply {
                            putExtra(Constants.KEY_DATA, JSONParser.serializer(result.data))
                        })
                        finish()
                    }
                    is LocationSearchResult.Failed,
                    null -> {
                        // no-op
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        searchBarBinding.searchView.requestFocus()
    }

    override fun onPause() {
        hideInputMethod(searchBarBinding.searchView)
        searchBarBinding.searchView.clearFocus()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private val recyclerClickListener = object : ListAdapterOnClickInterface<LocationQuery> {
        override fun onClick(view: View, item: LocationQuery) {
            if (item != LocationQuery.EMPTY) {
                locationSearchViewModel.onLocationSelected(item)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.searchProgressBar.isIndeterminate = show
        binding.recyclerView.isEnabled = !show
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        private var mY = 0
        private var shouldCloseKeyboard = false

        override fun onDown(e: MotionEvent): Boolean {
            mY = e.y.toInt()
            return super.onDown(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            setResult(RESULT_CANCELED)
            onBackPressedDispatcher.onBackPressed()
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
                hideInputMethod(binding.recyclerView)
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
                hideInputMethod(binding.recyclerView)
                shouldCloseKeyboard = false
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    override fun onThemeChanged(mode: UserThemeMode) {
        updateWindowColors(mode)
    }

    override fun updateWindowColors() {
        updateWindowColors(settingsManager.getUserThemeMode())
    }

    private fun updateWindowColors(mode: UserThemeMode) {
        var backgroundColor = getAttrColor(android.R.attr.colorBackground)
        var navBarColor = getAttrColor(R.attr.colorSurface)
        if (mode == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
            navBarColor = Colors.BLACK
        }

        binding.root.setBackgroundColor(backgroundColor)
        if (binding.appBar.background is MaterialShapeDrawable) {
            val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor = ColorStateList.valueOf(navBarColor)
        } else {
            binding.appBar.setBackgroundColor(navBarColor)
        }

        window.setTransparentWindow(
            backgroundColor, Colors.TRANSPARENT,
            if (getOrientation() == Configuration.ORIENTATION_PORTRAIT || isSmallestWidth(600)) {
                Colors.TRANSPARENT
            } else {
                backgroundColor
            }
        )
        window.setFullScreen(
            getOrientation() == Configuration.ORIENTATION_PORTRAIT || isSmallestWidth(
                600
            )
        )
    }

    fun fetchLocations(queryString: String?) {
        locationSearchViewModel.fetchLocations(queryString)
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                Snackbar.make(binding.root, error.stringId, Snackbar.LENGTH_SHORT).apply {
                    addCallback(SnackbarWindowAdjustCallback(this@LocationSearchActivity))
                }.show()
            }
            is ErrorMessage.String -> {
                Snackbar.make(binding.root, error.message, Snackbar.LENGTH_SHORT).apply {
                    addCallback(SnackbarWindowAdjustCallback(this@LocationSearchActivity))
                }.show()
            }
            is ErrorMessage.WeatherError -> {
                Snackbar.make(binding.root, error.exception.message, Snackbar.LENGTH_SHORT).apply {
                    addCallback(SnackbarWindowAdjustCallback(this@LocationSearchActivity))
                }.show()
            }
        }

        locationSearchViewModel.setErrorMessageShown(error)
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