package com.thewizrd.simpleweather.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R as materialRes
import com.google.android.material.search.SearchView
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
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.ActivityLocationSearchBinding
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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

class LocationSearchActivity : WindowColorActivity() {
    companion object {
        private const val TAG = "LocSearchFragment"

        const val RESULT_SUCCESS = Activity.RESULT_OK
        const val RESULT_CANCELED = Activity.RESULT_CANCELED
        const val RESULT_ALREADY_EXISTS = -2
    }

    private lateinit var binding: ActivityLocationSearchBinding

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
        binding.lifecycleOwner = this
        setContentView(binding.root)

        ViewCompat.setTransitionName(binding.root, null)
        ViewGroupCompat.setTransitionGroup((binding.root as ViewGroup), true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.root.updatePaddingRelative(
                start = sysBarInsets.left,
                end = sysBarInsets.right,
                top = sysBarInsets.top,
                bottom = sysBarInsets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        // Initialize
        binding.searchView.setVisible(true)
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.HIDING || newState == SearchView.TransitionState.HIDDEN) {
                setResult(RESULT_CANCELED)
                onBackPressedDispatcher.onBackPressed()
            }
        }
        binding.searchView.editText.addTextChangedListener(object : TextWatcher {
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
                fetchLocations(newText)
            }
        })
        binding.searchView.editText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                fetchLocations(v.text.toString())
                binding.searchView.clearFocusAndHideKeyboard()
                return@OnEditorActionListener true
            }
            false
        })
        binding.searchView.onItemClickListener = recyclerClickListener

        val padding = dpToPx(8f).toInt()
        binding.searchView.recyclerView.updatePaddingRelative(
            start = padding, end = padding, top = padding
        )

        val color = getAttrColor(materialRes.attr.colorPrimarySurface)
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
                binding.searchView.showLoading(loading)
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.locations.collectLatest {
                binding.searchView.submitList(it)
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

    override fun onResume() {
        super.onResume()
        binding.searchView.requestFocusAndShowKeyboard()
    }

    override fun onPause() {
        binding.searchView.clearFocusAndHideKeyboard()
        super.onPause()
    }

    private val recyclerClickListener = object : ListAdapterOnClickInterface<LocationQuery> {
        override fun onClick(view: View, item: LocationQuery) {
            if (item != LocationQuery.EMPTY) {
                locationSearchViewModel.onLocationSelected(item)
            }
        }
    }

    override fun onThemeChanged(mode: UserThemeMode) {
        updateWindowColors(mode)
    }

    override fun updateWindowColors() {
        updateWindowColors(settingsManager.getUserThemeMode())
    }

    private fun updateWindowColors(mode: UserThemeMode) {
        var backgroundColor = getAttrColor(materialRes.attr.colorSurfaceContainerHigh)
        if (mode == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
        }

        binding.root.setBackgroundColor(backgroundColor)
        binding.searchView.setBackgroundOverlayColor(backgroundColor)

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
        binding.searchView.clearFocusAndHideKeyboard()

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
}