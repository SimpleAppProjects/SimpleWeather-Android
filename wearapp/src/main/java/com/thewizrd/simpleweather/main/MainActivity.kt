package com.thewizrd.simpleweather.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.support.wearable.input.RotaryEncoder
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager.widget.ViewPager
import androidx.wear.widget.drawer.WearableDrawerLayout
import androidx.wear.widget.drawer.WearableDrawerView
import androidx.wear.widget.drawer.WearableNavigationDrawerView
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.ForecastsListViewModel
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.simpleweather.NavGraphDirections
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.databinding.ActivityMainBinding
import com.thewizrd.simpleweather.wearable.WearableListenerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : WearableListenerActivity(), MenuItem.OnMenuItemClickListener, WearableNavigationDrawerView.OnItemSelectedListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mNavController: NavController
    private lateinit var mNavDrawerAdapter: NavDrawerAdapter

    private var mItemSelectedRunnable: Runnable? = null

    private val weatherNowView: WeatherNowViewModel by viewModels()
    private val forecastsView: ForecastsListViewModel by viewModels()
    private val forecastPanelsView: ForecastPanelsViewModel by viewModels()
    private val alertsView: WeatherAlertsViewModel by viewModels()

    override lateinit var broadcastReceiver: BroadcastReceiver
    override lateinit var intentFilter: IntentFilter

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(ContextUtils.getThemeContextOverride(newBase, false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.activityMain.setDrawerStateCallback(object : WearableDrawerLayout.DrawerStateCallback() {
            override fun onDrawerOpened(layout: WearableDrawerLayout, drawerView: WearableDrawerView) {
                super.onDrawerOpened(layout, drawerView)
                drawerView.requestFocus()
            }

            override fun onDrawerClosed(layout: WearableDrawerLayout, drawerView: WearableDrawerView) {
                super.onDrawerClosed(layout, drawerView)
                drawerView.clearFocus()
            }

            override fun onDrawerStateChanged(layout: WearableDrawerLayout, newState: Int) {
                super.onDrawerStateChanged(layout, newState)
                if (newState != WearableDrawerView.STATE_IDLE && mItemSelectedRunnable != null) {
                    mItemSelectedRunnable!!.run()
                    mItemSelectedRunnable = null
                }
                if (newState == WearableDrawerView.STATE_IDLE &&
                        binding.bottomActionDrawer.isPeeking && binding.bottomActionDrawer.hasFocus()) {
                    binding.bottomActionDrawer.clearFocus()
                }
            }
        })

        binding.bottomActionDrawer.setOnMenuItemClickListener(this)
        binding.bottomActionDrawer.isPeekOnScrollDownEnabled = true

        binding.topNavDrawer.addOnItemSelectedListener(this)
        binding.topNavDrawer.isPeekOnScrollDownEnabled = true
        binding.topNavDrawer.setOnGenericMotionListener(object : OnGenericMotionListener {
            val pager: ViewPager? = binding.topNavDrawer.findViewById(R.id.ws_navigation_drawer_view_pager)
            val timer = object : CountDownTimer(200, 200) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    if (pager?.isFakeDragging == true) {
                        pager.endFakeDrag()
                    }
                    xTotalOffset = 0f
                }
            }
            var xTotalOffset = 0f

            override fun onGenericMotion(v: View, event: MotionEvent): Boolean {
                if (pager != null && event.action == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {
                    timer.cancel()
                    // Send event to postpone auto close of drawer
                    binding.topNavDrawer.onInterceptTouchEvent(event)

                    // Don't forget the negation here
                    val delta = RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(this@MainActivity)
                    if (Math.signum(delta) != Math.signum(xTotalOffset)) {
                        timer.onFinish()
                        xTotalOffset = delta * 1.5f
                    } else {
                        xTotalOffset += delta * 1.5f
                    }

                    if (pager.isFakeDragging || pager.beginFakeDrag()) {
                        pager.fakeDragBy(xTotalOffset)
                        if (Math.abs(xTotalOffset) >= pager.measuredWidth) {
                            timer.onFinish()
                        } else {
                            timer.start()
                        }
                    }

                    return true
                }
                return false
            }
        })
        mNavDrawerAdapter = NavDrawerAdapter(this)
        binding.topNavDrawer.setAdapter(mNavDrawerAdapter)

        forecastsView.getForecasts()?.observe(this, {
            lifecycleScope.launch {
                mNavDrawerAdapter.updateNavDrawerItems()
            }
        })
        forecastsView.getHourlyForecasts()?.observe(this, {
            lifecycleScope.launch {
                mNavDrawerAdapter.updateNavDrawerItems()
            }
        })
        alertsView.getAlerts()?.observe(this, {
            lifecycleScope.launch {
                mNavDrawerAdapter.updateNavDrawerItems()
            }
        })
        forecastPanelsView.getMinutelyForecasts()?.observe(this, {
            lifecycleScope.launch {
                mNavDrawerAdapter.updateNavDrawerItems()
            }
        })

        initWearableSyncReceiver()

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Check if fragment exists
        if (fragment == null) {
            val args = Bundle()
            if (intent?.hasExtra(Constants.KEY_DATA) == true) {
                args.putString(Constants.KEY_DATA, intent.getStringExtra(Constants.KEY_DATA))
            }

            val hostFragment = NavHostFragment.create(R.navigation.nav_graph, args)

            // Navigate to WeatherNowFragment
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        mNavController = findNavController(R.id.fragment_container)
        mNavController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.topNavDrawer.setCurrentItem(mNavDrawerAdapter.getDestinationPosition(destination.id), false)
        }
    }

    override fun onBackPressed() {
        var current: Fragment? = null
        if (supportFragmentManager.primaryNavigationFragment != null) {
            current = supportFragmentManager.primaryNavigationFragment!!.childFragmentManager.primaryNavigationFragment
        }
        var fragBackPressedListener: OnBackPressedFragmentListener? = null
        if (current is OnBackPressedFragmentListener) fragBackPressedListener = current

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_changelocation -> {
                mNavController.navigate(NavGraphDirections.actionGlobalSetupActivity())
            }
            R.id.menu_settings -> {
                mNavController.navigate(NavGraphDirections.actionGlobalSettingsActivity())
            }
            R.id.menu_openonphone -> {
                lifecycleScope.launch(Dispatchers.Main.immediate) {
                    openAppOnPhone(true)
                }
            }
        }
        return true
    }

    override fun onItemSelected(position: Int) {
        mItemSelectedRunnable = Runnable {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                when (mNavDrawerAdapter.getStringId(position)) {
                    R.string.label_condition -> {
                        mNavController.popBackStack(R.id.weatherNowFragment, false)
                    }
                    R.string.title_fragment_alerts -> {
                        mNavController.navigate(NavGraphDirections.actionGlobalWeatherAlertsFragment())
                    }
                    R.string.label_forecast -> {
                        mNavController.navigate(NavGraphDirections.actionGlobalWeatherForecastFragment())
                    }
                    R.string.label_hourlyforecast -> {
                        mNavController.navigate(NavGraphDirections.actionGlobalWeatherHrForecastFragment())
                    }
                    R.string.label_precipitation -> {
                        mNavController.navigate(NavGraphDirections.actionGlobalWeatherPrecipForecastFragment())
                    }
                    R.string.label_details -> {
                        mNavController.navigate(NavGraphDirections.actionGlobalWeatherDetailsFragment())
                    }
                    else -> {
                        mNavController.popBackStack(R.id.weatherNowFragment, false)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("$TAG: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("$TAG: onPause")
        super.onPause()
    }

    private inner class NavDrawerAdapter(private val mContext: Context) : WearableNavigationDrawerView.WearableNavigationDrawerAdapter() {
        private val navDrawerItems = listOf(
            NavDrawerItem(
                R.id.weatherNowFragment,
                R.string.label_nav_weathernow,
                R.drawable.wi_day_cloudy
            ),
            NavDrawerItem(
                R.id.weatherAlertsFragment,
                R.string.title_fragment_alerts,
                R.drawable.ic_error_white
            ),
            NavDrawerItem(
                R.id.weatherForecastFragment,
                R.string.label_forecast,
                R.drawable.ic_date_range_black_24dp
            ),
            NavDrawerItem(
                R.id.weatherHrForecastFragment,
                R.string.label_hourlyforecast,
                R.drawable.ic_access_time_black_24dp
            ),
            NavDrawerItem(
                R.id.weatherPrecipForecastFragment,
                R.string.label_precipitation,
                R.drawable.wi_raindrops
            ),
            NavDrawerItem(
                R.id.weatherDetailsFragment,
                R.string.label_details,
                R.drawable.ic_list_black_24dp
            )
        )
        private var navItems: List<NavDrawerItem>

        init {
            navItems = navDrawerItems
        }

        override fun getCount(): Int {
            return navItems.size
        }

        override fun getItemDrawable(pos: Int): Drawable {
            val drawable = ContextCompat.getDrawable(mContext, navItems[pos].drawableIcon)!!.mutate()
            drawable.setTint(ContextUtils.getColor(mContext, R.attr.colorOnSurface))
            return drawable
        }

        override fun getItemText(pos: Int): CharSequence {
            return mContext.getString(navItems[pos].titleString)
        }

        fun getStringId(pos: Int): Int {
            return navItems[pos].titleString
        }

        fun getDestinationId(pos: Int): Int {
            return navItems[pos].destinationId
        }

        fun getDestinationPosition(destinationId: Int): Int {
            return navItems.indexOfFirst { input ->
                input.destinationId == destinationId
            }
        }

        suspend fun updateNavDrawerItems() {
            navItems = withContext(Dispatchers.Default) {
                val items: MutableList<NavDrawerItem> = ArrayList(navDrawerItems)
                for (item in navDrawerItems) {
                    if (item.titleString == R.string.title_fragment_alerts &&
                        (alertsView.getAlerts()?.value.isNullOrEmpty())
                    ) {
                        items.remove(item)
                    }
                    if (item.titleString == R.string.label_forecast &&
                        (forecastsView.getForecasts()?.value.isNullOrEmpty())
                    ) {
                        items.remove(item)
                    }
                    if (item.titleString == R.string.label_hourlyforecast &&
                        (forecastsView.getHourlyForecasts()?.value.isNullOrEmpty())
                    ) {
                        items.remove(item)
                    }
                    if (item.titleString == R.string.label_precipitation &&
                        (forecastPanelsView.getMinutelyForecasts()?.value.isNullOrEmpty())
                    ) {
                        items.remove(item)
                    }
                }
                items
            }

            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    private class NavDrawerItem(val destinationId: Int, val titleString: Int, val drawableIcon: Int)

    /* Data Sync */
    private fun initWearableSyncReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                lifecycleScope.launch {
                    if (ACTION_OPENONPHONE == intent.action) {
                        val showAni = intent.getBooleanExtra(EXTRA_SHOWANIMATION, false)
                        openAppOnPhone(showAni)
                    } else if (ACTION_REQUESTSETUPSTATUS == intent.action) {
                        sendSetupStatusRequest()
                    }
                }
            }
        }

        intentFilter = IntentFilter().apply {
            addAction(ACTION_OPENONPHONE)
            addAction(ACTION_REQUESTSETUPSTATUS)
        }
    }
}