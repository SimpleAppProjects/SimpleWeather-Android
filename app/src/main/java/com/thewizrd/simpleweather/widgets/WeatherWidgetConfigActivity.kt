package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.thewizrd.common.utils.ActivityUtils.setFullScreen
import com.thewizrd.common.utils.ActivityUtils.setTransparentWindow
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.ActivityWidgetSetupBinding
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.widgets.preferences.WeatherWidget4x3LocationFragment
import com.thewizrd.simpleweather.widgets.preferences.WeatherWidgetPreferenceFragment

class WeatherWidgetConfigActivity : UserLocaleActivity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: ActivityWidgetSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("WidgetConfig: onCreate")

        // Find the widget id from the intent.
        if (intent?.extras != null) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        )

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish()
            return
        }

        binding = ActivityWidgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = sysBarInsets.left
                rightMargin = sysBarInsets.right
                if (isSmallestWidth(600)) {
                    topMargin = sysBarInsets.top
                    bottomMargin = sysBarInsets.bottom
                }
            }

            WindowInsetsCompat.Builder(insets)
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(0, sysBarInsets.top, 0, sysBarInsets.bottom)
                )
                .build()
        }

        var color = getAttrColor(android.R.attr.colorBackground)
        if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }

        window.setTransparentWindow(
            color,
            Colors.TRANSPARENT,
            ColorUtils.setAlphaComponent(color, 0xB3)
        )
        window.setFullScreen(true)

        val mWidgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)

        val args = Bundle().apply {
            putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)

            if (intent?.extras != null) {
                putAll(intent.extras)
            }

            if (!containsKey(WeatherWidgetProvider.EXTRA_LOCATIONQUERY) && WidgetUtils.exists(
                    mAppWidgetId
                )
            ) {
                WidgetUtils.getLocationData(mAppWidgetId)?.let {
                    putString(WeatherWidgetProvider.EXTRA_LOCATIONNAME, it.name)
                    putString(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, it.query)
                }
            }
        }

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val fragment = if (mWidgetType == WidgetType.Widget4x3Locations) {
                WeatherWidget4x3LocationFragment()
            } else {
                WeatherWidgetPreferenceFragment()
            }.apply {
                arguments = args
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }

        // Update configuration
        remoteConfigService.checkConfig()
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