package com.thewizrd.simpleweather.setup

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.TransitionManager
import com.thewizrd.common.utils.ActivityUtils.setTransparentWindow
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.SetupGraphDirections
import com.thewizrd.simpleweather.databinding.ActivitySetupBinding
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.stepper.StepperFragment
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import kotlinx.coroutines.launch

class SetupActivity : UserLocaleActivity() {
    private lateinit var binding: ActivitySetupBinding
    private val viewModel: SetupViewModel by viewModels()
    private var mNavController: NavController? = null
    private var isWeatherLoaded = false

    // Widget id for ConfigurationActivity
    private var mAppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupActivity: onCreate")

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavBar) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                leftMargin = sysBarInsets.left
                rightMargin = sysBarInsets.right
                bottomMargin = sysBarInsets.bottom
            }

            insets
        }

        val color = getAttrColor(R.attr.colorPrimarySurface)
        window.setTransparentWindow(color)

        lifecycleScope.launch {
            isWeatherLoaded = settingsManager.isWeatherLoaded()

            // Check if this activity was started from adding a new widget
            if (AppWidgetManager.ACTION_APPWIDGET_CONFIGURE == intent?.action) {
                mAppWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

                if (settingsManager.isWeatherLoaded() || mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // This shouldn't happen, but just in case
                    setResult(Activity.RESULT_OK)
                    finish()
                    // Return if we're finished
                    return@launch
                }

                // Set the result to CANCELED.  This will cause the widget host to cancel
                // out of the widget placement if they press the back button.
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                )
            }

            if (savedInstanceState?.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID) == true
                && mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID
            ) {
                mAppWidgetId =
                    savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                // Set the result to CANCELED.  This will cause the widget host to cancel
                // out of the widget placement if they press the back button.
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                )
            }

            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (fragment == null) {
                val hostFragment = NavHostFragment.create(R.navigation.setup_graph)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commitNowAllowingStateLoss() // Commit now; we need the NavController immediately after
            }

            if (isWeatherLoaded && viewModel.locationData == null) {
                viewModel.locationData = settingsManager.getHomeData()
            }

            setupBottomNavBar()
            initializeNavController()
        }
    }

    override fun onStart() {
        super.onStart()

        if (mNavController == null) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment != null) {
                initializeNavController()
            }
        }
    }

    private fun initializeNavController() {
        // Don't initialize the controller to early
        // The fragment may not have called onCreate yet; which is where the NavController gets assigned
        // It should be ready once we reach onStart
        lifecycleScope.launchWhenStarted {
            mNavController = getNavController()
            mNavController!!.addOnDestinationChangedListener { _, destination, _ ->
                updateBottomNavigationBarForDestination(destination.id)
            }
        }
    }

    private fun getNavController(): NavController {
        var mNavController: NavController? = null
        try {
            mNavController = findNavController(R.id.fragment_container)
        } catch (e: IllegalStateException) {
            // View not yet created
            // Retrieve by fragment
        } catch (e: IllegalArgumentException) {
            // View not yet created
            // Retrieve by fragment
        }

        if (mNavController == null) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment !is NavHostFragment) {
                throw IllegalStateException("NavHostFragment not yet initialized!")
            } else {
                mNavController = fragment.navController
            }
        }

        return mNavController
    }

    private fun setupBottomNavBar() {
        binding.bottomNavBar.setItemCount(itemCount)
        binding.bottomNavBar.setSelectedItem(0)
        binding.bottomNavBar.visibility = View.VISIBLE
        binding.bottomNavBar.setOnBackButtonClickListener {
            mNavController?.navigateUp()
        }
        binding.bottomNavBar.setOnNextButtonClickListener {
            val current =
                supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.primaryNavigationFragment
            if (current is StepperFragment && !current.canGoNext())
                return@setOnNextButtonClickListener

            val destination = mNavController?.currentDestination
            if (destination != null) {
                @IdRes val destinationId: Int = destination.id
                if (getPosition(destinationId) >= itemCount - 1) {
                    // Complete
                    onCompleted()
                } else {
                    val nextDestination = getNextDestination(destinationId)
                    if (nextDestination != R.id.setupSettingsFragment || viewModel.locationData != null) {
                        val navOpts = NavOptions.Builder()

                        if (nextDestination == R.id.setupSettingsFragment)
                            navOpts.setPopUpTo(destinationId, true)

                        mNavController?.navigate(nextDestination, null, navOpts.build())
                    }
                }
            }
        }
    }

    private val itemCount: Int
        get() = if (isWeatherLoaded) {
            2
        } else {
            if (BuildConfig.IS_NONGMS) {
                4
            } else {
                3
            }
        }

    private fun getPosition(@IdRes destinationId: Int): Int {
        return when (destinationId) {
            R.id.setupWelcomeFragment -> {
                0
            }
            R.id.setupProviderFragment -> {
                1
            }
            R.id.setupLocationFragment,
            R.id.locationSearchFragment3 -> {
                if (BuildConfig.IS_NONGMS) {
                    2
                } else {
                    1
                }
            }
            R.id.setupSettingsFragment -> {
                if (isWeatherLoaded) {
                    1
                } else {
                    if (BuildConfig.IS_NONGMS) {
                        3
                    } else {
                        2
                    }
                }
            }
            else -> 0
        }
    }

    @IdRes
    private fun getNextDestination(@IdRes destinationId: Int): Int {
        return when (destinationId) {
            R.id.setupWelcomeFragment -> {
                if (isWeatherLoaded) {
                    R.id.setupSettingsFragment
                } else {
                    if (BuildConfig.IS_NONGMS) {
                        R.id.setupProviderFragment
                    } else {
                        R.id.setupLocationFragment
                    }
                }
            }
            R.id.setupProviderFragment -> R.id.setupLocationFragment
            R.id.setupLocationFragment,
            R.id.locationSearchFragment3 -> {
                R.id.setupSettingsFragment
            }
            R.id.setupSettingsFragment -> R.id.mainActivity
            else -> {
                if (isWeatherLoaded) {
                    R.id.setupSettingsFragment
                } else {
                    if (BuildConfig.IS_NONGMS) {
                        R.id.setupProviderFragment
                    } else {
                        R.id.setupLocationFragment
                    }
                }
            }
        }
    }

    private fun updateBottomNavigationBarForDestination(@IdRes destinationId: Int) {
        binding.bottomNavBar.setSelectedItem(getPosition(destinationId))
        if (destinationId == R.id.setupLocationFragment || destinationId == R.id.locationSearchFragment3) {
            binding.bottomNavBar.showBackButton(BuildConfig.IS_NONGMS)
            binding.bottomNavBar.showNextButton(false)
        } else if (destinationId == R.id.setupSettingsFragment) {
            binding.bottomNavBar.showBackButton(false)
        }
        binding.bottomNavBar.postOnAnimationDelayed({
            if (destinationId == R.id.setupLocationFragment || destinationId == R.id.locationSearchFragment3) {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            }
            binding.bottomNavBar.visibility =
                if (destinationId == R.id.locationSearchFragment3) View.GONE else View.VISIBLE
        }, (Constants.ANIMATION_DURATION * 1.5f).toLong())
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupActivity: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupActivity: onPause")
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
    }

    private fun onCompleted() {
        lifecycleScope.launch {
            // Completion
            settingsManager.setWeatherLoaded(true)
            settingsManager.setOnBoardingComplete(true)

            if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Start WeatherNow Activity with weather data
                val opts = NavOptions.Builder()
                mNavController?.currentDestination?.let { destination ->
                    opts.setPopUpTo(destination.id, true)
                }
                mNavController?.safeNavigate(
                    SetupGraphDirections.actionGlobalMainActivity()
                        .setData(
                            JSONParser.serializer(viewModel.locationData, LocationData::class.java)
                        ),
                    opts.build()
                )

                // We have an invalid widget id but just in case
                setResult(Activity.RESULT_CANCELED)
                finishAffinity()
            } else {
                // Create return intent
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                    viewModel.locationData?.let {
                        putExtra(
                            Constants.KEY_DATA,
                            JSONParser.serializer(it, LocationData::class.java)
                        )
                    }
                }
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            }
        }
    }
}