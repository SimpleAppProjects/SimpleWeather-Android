package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.navigation.fragment.NavHostFragment
import com.thewizrd.shared_resources.helpers.ActivityUtils
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity

class WeatherWidgetConfigActivity : UserLocaleActivity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("WidgetConfig: onCreate")

        // Find the widget id from the intent.
        if (intent?.extras != null) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId))

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish()
        }

        setContentView(R.layout.activity_widget_setup)

        var color = ContextUtils.getColor(this, android.R.attr.colorBackground)
        if (App.instance.settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }

        ActivityUtils.setTransparentWindow(
            window,
            color,
            Colors.TRANSPARENT,
            ColorUtils.setAlphaComponent(color, 0xB3)
        )
        ActivityUtils.setFullScreen(window, true)

        val args = Bundle().apply {
            putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        }

        if (intent?.extras != null) {
            args.putAll(intent.extras)
        }

        if (intent?.extras?.containsKey(WeatherWidgetProvider.EXTRA_LOCATIONQUERY) == false && WidgetUtils.exists(
                mAppWidgetId
            )
        ) {
            WidgetUtils.getLocationData(mAppWidgetId)?.let {
                args.putString(WeatherWidgetProvider.EXTRA_LOCATIONNAME, it.name)
                args.putString(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, it.query)
            }
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            val hostFragment = NavHostFragment.create(R.navigation.widget_graph, args)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, hostFragment)
                .setPrimaryNavigationFragment(hostFragment)
                .commit()
        }

        // Update configuration
        RemoteConfig.checkConfig()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        super.onSaveInstanceState(outState)
    }
}