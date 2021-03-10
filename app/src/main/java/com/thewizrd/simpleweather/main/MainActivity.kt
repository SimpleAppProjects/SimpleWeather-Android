package com.thewizrd.simpleweather.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.transition.TransitionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.helpers.ActivityUtils
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.UserThemeMode.OnThemeChangeListener
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.activity.UserLocaleActivity
import com.thewizrd.simpleweather.databinding.ActivityMainBinding
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.services.UpdaterUtils
import com.thewizrd.simpleweather.shortcuts.ShortcutCreatorWorker
import com.thewizrd.simpleweather.updates.InAppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : UserLocaleActivity(),
        BottomNavigationView.OnNavigationItemSelectedListener,
        OnThemeChangeListener {
    companion object {
        private const val TAG = "MainActivity"
        private const val INSTALL_REQUESTCODE = 168
    }

    private lateinit var binding: ActivityMainBinding
    private var mNavController: NavController? = null

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private var appUpdateManager: InAppUpdateManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("MainActivity: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavBar.setOnNavigationItemSelectedListener(this)

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavBar, object : OnApplyWindowInsetsListener {
            private val paddingStart = ViewCompat.getPaddingStart(binding.bottomNavBar)
            private val paddingTop = binding.bottomNavBar.paddingTop
            private val paddingEnd = ViewCompat.getPaddingEnd(binding.bottomNavBar)
            private val paddingBottom = binding.bottomNavBar.paddingBottom
            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                ViewCompat.setPaddingRelative(v,
                        paddingStart + insets.systemWindowInsetLeft,
                        paddingTop,
                        paddingEnd + insets.systemWindowInsetRight,
                        paddingBottom + insets.systemWindowInsetBottom)
                return insets
            }
        })

        binding.bottomNavBar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(view.paddingLeft,
                        0,
                        view.width - view.paddingRight,
                        view.height)
            }
        }

        // Back stack listener
        supportFragmentManager.addOnBackStackChangedListener { refreshNavViewCheckedItem() }

        updateWindowColors(Settings.getUserThemeMode())

        val args = Bundle()
        if (intent != null && intent.extras != null) {
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
                        args.getString(Constants.KEY_DATA), LocationData::class.java)
                args.putBoolean(Constants.FRAGTAG_HOME, ObjectsCompat.equals(locData, Settings.getHomeData()))
            }
        }

        // Start services
        UpdaterUtils.startAlarm(this)

        if (FeatureSettings.isUpdateAvailable()) {
            // Update is available; double check if mandatory
            appUpdateManager = InAppUpdateManager.create(applicationContext)
            lifecycleScope.launch {
                val isUpdateAvailable = appUpdateManager!!.shouldStartImmediateUpdateFlow()
                if (isUpdateAvailable) {
                    appUpdateManager!!.startImmediateUpdateFlow(this@MainActivity, INSTALL_REQUESTCODE)
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        initializeNavFragment(args)
                        // Don't initialize the controller to early
                        // The fragment may not have called onCreate yet; which is where the NavController gets assigned
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            initializeNavController()
                        }
                    }
                }
            }
            return
        }
        initializeNavFragment(args)
    }

    private fun initializeNavFragment(args: Bundle) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            val hostFragment = NavHostFragment.create(R.navigation.nav_graph, args)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commit()
        }
    }

    override fun onStart() {
        super.onStart()

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            initializeNavController()
        }

        // Update app shortcuts
        ShortcutCreatorWorker.requestUpdateShortcuts(this)
    }

    private fun initializeNavController() {
        mNavController = Navigation.findNavController(this, R.id.fragment_container)

        binding.bottomNavBar.setupWithNavController(mNavController!!)
        mNavController!!.addOnDestinationChangedListener { controller, destination, arguments ->
            refreshNavViewCheckedItem()

            if (destination.id == R.id.weatherNowFragment || destination.id == R.id.locationsFragment) {
                binding.bottomNavBar.visibility = View.VISIBLE
            } else {
                binding.bottomNavBar.postOnAnimationDelayed({
                    if (destination.id == R.id.locationSearchFragment3 || destination.id == R.id.weatherNowFragment) {
                        TransitionManager.beginDelayedTransition((binding.root as ViewGroup))
                    }
                    binding.bottomNavBar.visibility = if (destination.id == R.id.locationSearchFragment) View.GONE else View.VISIBLE
                }, (Constants.ANIMATION_DURATION * 1.5f).toLong())
            }
        }

        // Alerts: from weather alert notification
        if (intent != null && WeatherAlertNotificationService.ACTION_SHOWALERTS == intent.action) {
            val destination = mNavController!!.currentDestination
            if (destination != null && destination.id != R.id.weatherListFragment) {
                val locationData = Settings.getHomeData()
                val args = WeatherListFragmentDirections.actionGlobalWeatherListFragment()
                        .setData(JSONParser.serializer(locationData, LocationData::class.java))
                        .setWeatherListType(WeatherListType.ALERTS)
                mNavController!!.navigate(args)
            }
        }

        // Check nav item in bottom nav view
        // based on current fragment
        refreshNavViewCheckedItem()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Alerts: from weather alert notification
        if (WeatherAlertNotificationService.ACTION_SHOWALERTS == intent.action) {
            val args = Bundle()
            if (intent.extras != null) {
                args.putAll(intent.extras)
            }
            args.putSerializable(Constants.ARGS_WEATHERLISTTYPE, WeatherListType.ALERTS)
            mNavController!!.navigate(R.id.weatherListFragment, args)
        }
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.primaryNavigationFragment

        var fragBackPressedListener: OnBackPressedFragmentListener? = null
        if (current is OnBackPressedFragmentListener)
            fragBackPressedListener = current

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        if (mNavController!!.currentDestination != null && id != mNavController!!.currentDestination!!.id) {
            item.onNavDestinationSelected(mNavController!!)
        }

        return true
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        refreshNavViewCheckedItem()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("MainActivity: onResume")

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (FeatureSettings.isUpdateAvailable()) {
            appUpdateManager!!.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE)
        }

        val container = binding.fragmentContainer.findViewById<View>(R.id.radar_webview_container)
        if (container is ViewGroup && container.getChildAt(0) is WebView) {
            val webView = container.getChildAt(0) as WebView
            webView.resumeTimers()
        }
    }

    override fun onPause() {
        super.onPause()
        AnalyticsLogger.logEvent("MainActivity: onPause")
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
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("RestrictedApi")
    private fun refreshNavViewCheckedItem() {
        if (mNavController != null && mNavController!!.currentDestination != null) {
            val currentId = mNavController!!.currentDestination!!.id
            val currentName = if (mNavController!!.currentDestination is FragmentNavigator.Destination) {
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

            val item = binding.bottomNavBar.menu.findItem(checkedItemId)
            if (item != null) {
                item.isChecked = true
            }
        }
    }

    override fun onThemeChanged(mode: UserThemeMode) {
        updateWindowColors(mode)
    }

    private fun updateWindowColors(mode: UserThemeMode) {
        var color = ContextUtils.getColor(this, android.R.attr.colorBackground)
        if (mode == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }

        ActivityUtils.setTransparentWindow(window, color, Colors.TRANSPARENT, color)
        binding.root.setBackgroundColor(color)
        binding.bottomNavBar.setBackgroundColor(color)
    }
}