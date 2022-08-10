package com.thewizrd.simpleweather.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.MaterialShapeDrawable
import com.thewizrd.common.utils.ActivityUtils.setFullScreen
import com.thewizrd.common.utils.ActivityUtils.setTransparentWindow
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.UpdateSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.shared_resources.utils.UserThemeMode.OnThemeChangeListener
import com.thewizrd.simpleweather.NavGraphDirections
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.ActivityMainBinding
import com.thewizrd.simpleweather.helpers.WindowColorManager
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.services.UpdaterUtils
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.updates.InAppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : UserLocaleActivity(), OnThemeChangeListener, WindowColorManager {
    companion object {
        private const val TAG = "MainActivity"
        private const val INSTALL_REQUESTCODE = 168
    }

    private lateinit var binding: ActivityMainBinding
    private var mNavController: NavController? = null

    private var prevConfig: Configuration? = null

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private var appUpdateManager: InAppUpdateManager? = null

    private fun getNavBar(): NavigationBarView? {
        return binding.bottomNavBar ?: binding.navigationRail
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("$TAG: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isSmallestWidth(600)) {
            binding.fragmentContainer.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val cornerRadius = view.context.dpToPx(28f)
                    outline.setRoundRect(
                        0, 0,
                        view.width, view.height + cornerRadius.toInt(),
                        cornerRadius
                    )
                }
            }
            binding.fragmentContainer.clipToOutline = true
        }

        getNavBar()?.setOnItemSelectedListener { item ->
            // Handle navigation view item clicks here.
            val id = item.itemId

            if (mNavController?.currentDestination != null && id != mNavController!!.currentDestination!!.id) {
                item.onNavDestinationSelected(mNavController!!)
            }

            return@setOnItemSelectedListener true
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            if (isSmallestWidth(600)) {
                binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    topMargin = sysBarInsets.top
                    bottomMargin = sysBarInsets.bottom
                }
            }

            insets
        }

        binding.bottomNavBar?.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(
                    view.paddingLeft,
                    0,
                    view.width - view.paddingRight,
                    view.height
                )
            }
        }

        // Back stack listener
        supportFragmentManager.addOnBackStackChangedListener { refreshNavViewCheckedItem() }

        updateWindowColors()

        lifecycleScope.launch {
            val args = Bundle()
            if (intent?.extras != null) {
                args.putAll(intent.extras)
            }

            // Shortcut intent: from app shortcuts
            if (args.containsKey(Constants.KEY_SHORTCUTDATA)) {
                val data = args.getString(Constants.KEY_SHORTCUTDATA)
                args.remove(Constants.KEY_SHORTCUTDATA)
                args.putString(Constants.KEY_DATA, data)
            }

            if (args.containsKey(Constants.KEY_DATA)) {
                if (!args.containsKey(Constants.FRAGTAG_HOME)) {
                    val locData = JSONParser.deserializer(
                        args.getString(Constants.KEY_DATA), LocationData::class.java
                    )
                    args.putBoolean(
                        Constants.FRAGTAG_HOME,
                        ObjectsCompat.equals(locData, settingsManager.getHomeData())
                    )
                }
            }

            // Start services
            UpdaterUtils.startAlarm(this@MainActivity)

            if (UpdateSettings.isUpdateAvailable) {
                // Update is available; double check if mandatory
                appUpdateManager = InAppUpdateManager.create(applicationContext)
                val isUpdateAvailable = appUpdateManager!!.shouldStartImmediateUpdateFlow()
                if (isUpdateAvailable) {
                    appUpdateManager!!.startImmediateUpdateFlow(
                        this@MainActivity,
                        INSTALL_REQUESTCODE
                    )
                } else {
                    lifecycleScope.launch(Dispatchers.Main.immediate) {
                        // Commit transaction now, if needed, since we'll try to access
                        // the NavController almost immediately after
                        initializeNavFragment(args, true)
                        initializeNavController()
                    }
                }
                return@launch
            }
            initializeNavFragment(args)
        }
    }

    private fun initializeNavFragment(args: Bundle, commitNow: Boolean = false) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            val hostFragment = NavHostFragment.create(R.navigation.nav_graph, args)
            val transaction = supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)

            if (commitNow)
                transaction.commitNowAllowingStateLoss()
            else
                transaction.commitAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        prevConfig = Configuration(this.resources.configuration)

        AnalyticsLogger.logEvent("$TAG: onStart")

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            initializeNavController()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // Update app shortcuts
            ShortcutCreatorWorker.requestUpdateShortcuts(this)
        }
    }

    private fun initializeNavController() {
        // Don't initialize the controller to early
        // The fragment may not have called onCreate yet; which is where the NavController gets assigned
        // It should be ready once we reach onStart
        lifecycleScope.launchWhenStarted {
            mNavController = getNavController()

            getNavBar()?.setupWithNavController(mNavController!!)
            mNavController!!.addOnDestinationChangedListener { _, destination, _ ->
                refreshNavViewCheckedItem()

                if (destination.id == R.id.weatherNowFragment || destination.id == R.id.locationsFragment) {
                    getNavBar()?.visibility = View.VISIBLE
                } else {
                    getNavBar()?.postOnAnimationDelayed({
                        if (destination.id == R.id.weatherNowFragment) {
                            TransitionManager.beginDelayedTransition((binding.root as ViewGroup))
                        }
                    }, (Constants.ANIMATION_DURATION * 1.5f).toLong())
                }
            }

            // Alerts: from weather alert notification
            if (WeatherAlertNotificationService.ACTION_SHOWALERTS == intent?.action) {
                val destination = mNavController!!.currentDestination
                if (destination != null && destination.id != R.id.weatherListFragment) {
                    val locationData = settingsManager.getHomeData()
                    val args = NavGraphDirections.actionGlobalWeatherListFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                        .setWeatherListType(WeatherListType.ALERTS)
                    mNavController!!.navigate(args)
                }
            }

            // Check nav item in bottom nav view
            // based on current fragment
            refreshNavViewCheckedItem()
        }
    }

    private fun getNavController(): NavController {
        var mNavController: NavController? = null
        try {
            mNavController = findNavController(R.id.fragment_container)
        } catch (e: IllegalStateException) {
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Alerts: from weather alert notification
        if (WeatherAlertNotificationService.ACTION_SHOWALERTS == intent?.action) {
            val args = Bundle()
            if (intent.extras != null) {
                args.putAll(intent.extras)
            }
            args.putSerializable(Constants.ARGS_WEATHERLISTTYPE, WeatherListType.ALERTS)
            mNavController?.navigate(R.id.weatherListFragment, args)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        refreshNavViewCheckedItem()
    }

    override fun onResume() {
        super.onResume()

        AnalyticsLogger.logEvent("$TAG: onResume")

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (UpdateSettings.isUpdateAvailable) {
            appUpdateManager?.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE)
        }

        val container = binding.fragmentContainer.findViewById<View>(R.id.radar_webview_container)
        if (container is ViewGroup && container.getChildAt(0) is WebView) {
            val webView = container.getChildAt(0) as WebView
            webView.resumeTimers()
        }
    }

    override fun onPause() {
        super.onPause()

        AnalyticsLogger.logEvent("$TAG: onPause")

        val container = binding.fragmentContainer.findViewById<View>(R.id.radar_webview_container)
        if (container is ViewGroup && container.getChildAt(0) is WebView) {
            val webView = container.getChildAt(0) as WebView
            webView.pauseTimers()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == INSTALL_REQUESTCODE) {
            if (resultCode != RESULT_OK) {
                // Update flow failed; exit
                finishAffinity()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("RestrictedApi")
    private fun refreshNavViewCheckedItem() {
        if (mNavController?.currentDestination != null) {
            val currentId = mNavController!!.currentDestination!!.id
            val currentName = if (mNavController?.currentDestination is FragmentNavigator.Destination) {
                (mNavController!!.currentDestination as FragmentNavigator.Destination).className
            } else {
                mNavController!!.currentDestination!!.displayName
            }
            var checkedItemId = -1

            if (currentId == R.id.weatherNowFragment || currentId == R.id.weatherListFragment) {
                checkedItemId = R.id.weatherNowFragment
            } else if (currentId == R.id.weatherRadarFragment) {
                checkedItemId = R.id.weatherRadarFragment
            } else if (currentId == R.id.locationsFragment) {
                checkedItemId = R.id.locationsFragment
            } else if (currentName.contains(SettingsFragment::class.java.name)) {
                checkedItemId = R.id.settingsFragment
            }

            val item = getNavBar()?.menu?.findItem(checkedItemId)
            if (item != null) {
                item.isChecked = true
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
        var backgroundColor = getAttrColor(android.R.attr.colorBackground)
        var navBarColor = getAttrColor(R.attr.colorSurface)
        if (mode == UserThemeMode.AMOLED_DARK) {
            backgroundColor = Colors.BLACK
            navBarColor = Colors.BLACK
        }

        binding.root.setBackgroundColor(backgroundColor)
        if (getNavBar()?.background is MaterialShapeDrawable) {
            val materialShapeDrawable = getNavBar()?.background as MaterialShapeDrawable
            materialShapeDrawable.fillColor = ColorStateList.valueOf(navBarColor)
        } else {
            getNavBar()?.setBackgroundColor(navBarColor)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (prevConfig == null || (newConfig.diff(prevConfig) and ActivityInfo.CONFIG_ORIENTATION) != 0) {
            updateWindowColors(settingsManager.getUserThemeMode())
        }

        prevConfig = Configuration(newConfig)
    }
}