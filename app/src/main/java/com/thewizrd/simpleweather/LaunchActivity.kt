package com.thewizrd.simpleweather

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.thewizrd.extras.ExtrasLibrary.Companion.checkPremiumStatus
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.activity.UserLocaleActivity
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import com.thewizrd.simpleweather.updates.InAppUpdateManager
import kotlinx.coroutines.launch

class LaunchActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "LaunchActivity"
        private const val INSTALL_REQUESTCODE = 168
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private var appUpdateManager: InAppUpdateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FeatureSettings.isUpdateAvailable()) {
            // Update is available; double check if mandatory
            InAppUpdateManager.create(applicationContext).also {
                appUpdateManager = it

                lifecycleScope.launch {
                    if (it.shouldStartImmediateUpdateFlow()) {
                        it.startImmediateUpdateFlow(this@LaunchActivity, INSTALL_REQUESTCODE)
                    } else {
                        startMainActivity()
                    }
                }
            }
            return
        }

        // Update configuration
        RemoteConfig.checkConfig()

        // Check premium status
        checkPremiumStatus()
        startMainActivity()
    }

    override fun onResume() {
        super.onResume()

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (FeatureSettings.isUpdateAvailable()) {
            appUpdateManager?.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE)
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

    private fun startMainActivity() {
        val settingsMgr = App.instance.settingsManager
        var intent: Intent? = null

        try {
            intent = if (settingsMgr.isWeatherLoaded() && settingsMgr.isOnBoardingComplete()) {
                Intent(this, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Constants.KEY_DATA, JSONParser.serializer(settingsMgr.getHomeData(), LocationData::class.java))
                        .putExtra(Constants.FRAGTAG_HOME, true)
            } else {
                Intent(this, SetupActivity::class.java)
            }
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
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