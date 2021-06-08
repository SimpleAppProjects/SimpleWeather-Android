package com.thewizrd.simpleweather

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.thewizrd.extras.ExtrasLibrary
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaunchActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "LaunchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsMgr = App.instance.settingsManager

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
                RemoteConfig.checkConfig()

                // Check premium status
                ExtrasLibrary.checkPremiumStatus()
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