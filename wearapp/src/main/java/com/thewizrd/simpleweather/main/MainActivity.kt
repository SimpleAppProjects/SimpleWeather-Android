package com.thewizrd.simpleweather.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.ActivityMainBinding
import com.thewizrd.simpleweather.wearable.WearableListenerActivity
import kotlinx.coroutines.launch

class MainActivity : WearableListenerActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override lateinit var broadcastReceiver: BroadcastReceiver
    override lateinit var intentFilter: IntentFilter

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(newBase.getThemeContextOverride(false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("$TAG: onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initWearableSyncReceiver()

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WeatherNowFragment().apply {
                    if (intent?.hasExtra(Constants.KEY_DATA) == true) {
                        arguments = Bundle(1).apply {
                            putString(Constants.KEY_DATA, intent.getStringExtra(Constants.KEY_DATA))
                        }
                    }
                })
                .commit()
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