package com.thewizrd.simpleweather

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.UpdateSettings
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.extras.checkPremiumStatus
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.updates.InAppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaunchActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LaunchActivity"
        private const val INSTALL_REQUESTCODE = 168
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private var appUpdateManager: InAppUpdateManager? = null

    private var isReadyToView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

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

        if (UpdateSettings.isUpdateAvailable) {
            // Update is available; double check if mandatory
            InAppUpdateManager.create(applicationContext).also {
                appUpdateManager = it

                lifecycleScope.launch {
                    if (it.shouldStartImmediateUpdateFlow()) {
                        it.startImmediateUpdateFlow(this@LaunchActivity, INSTALL_REQUESTCODE)
                        isReadyToView = true
                    } else {
                        startMainActivity()
                    }
                }
            }
            return
        }

        lifecycleScope.launch {
            // Update configuration
            remoteConfigService.checkConfig()

            // Check premium status
            checkPremiumStatus()
            startMainActivity()
        }
    }

    override fun onResume() {
        super.onResume()

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (UpdateSettings.isUpdateAvailable) {
            appUpdateManager?.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE)
            isReadyToView = true
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

    private suspend fun startMainActivity() {
        var intent: Intent? = null

        try {
            intent =
                if (settingsManager.isWeatherLoaded() && settingsManager.isOnBoardingComplete()) {
                    Intent(this, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Constants.KEY_DATA, withContext(Dispatchers.Default) {
                            JSONParser.serializer(
                                settingsManager.getHomeData(),
                                LocationData::class.java
                            )
                        })
                        .putExtra(Constants.FRAGTAG_HOME, true)
                } else {
                    Intent(this, SetupActivity::class.java)
                }
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e, "%s: error loading", TAG)
        } finally {
            if (intent == null) {
                intent = Intent(this, SetupActivity::class.java)
            }

            // Navigate
            startActivity(intent)
            finishAffinity()
        }
    }
}