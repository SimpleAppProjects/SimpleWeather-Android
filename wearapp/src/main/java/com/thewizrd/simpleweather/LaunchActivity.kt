package com.thewizrd.simpleweather

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.thewizrd.extras.extrasModule
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaunchActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LaunchActivity"
    }

    private var isReadyToView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Stop activity from rendering until next activity or if immediate update available
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (isReadyToView) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    true
                } else {
                    false
                }
            }
        })

        val settingsMgr = appLib.settingsManager

        lifecycleScope.launch {
            var intent: Intent? = null

            try {
                intent = if (settingsMgr.isWeatherLoaded()) {
                    Intent(this@LaunchActivity, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Constants.KEY_DATA, withContext(Dispatchers.Default) {
                            JSONParser.serializer(
                                settingsMgr.getHomeData(),
                                LocationData::class.java
                            )
                        })
                        .putExtra(Constants.FRAGTAG_HOME, true)
                } else {
                    Intent(this@LaunchActivity, SetupActivity::class.java)
                }

                // Update configuration
                remoteConfigService.checkConfig()

                // Check premium status
                extrasModule.checkPremiumStatus()
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e, "%s: error loading", TAG)
            } finally {
                if (intent == null) {
                    intent = Intent(this@LaunchActivity, SetupActivity::class.java)
                }

                // Navigate
                startActivity(intent)
                finishAffinity()
            }
        }
    }
}