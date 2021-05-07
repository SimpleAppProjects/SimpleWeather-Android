package com.thewizrd.simpleweather.updates

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class InAppUpdateManager private constructor(context: Context) {
    companion object {
        @JvmStatic
        fun create(context: Context): InAppUpdateManager {
            return InAppUpdateManager(context)
        }
    }

    suspend fun checkIfUpdateAvailable(): Boolean {
        return false
    }

    private suspend fun checkIfUpdateAvailableFallback(): Boolean {
        return false
    }

    /**
     * Must call [InAppUpdateManager.checkIfUpdateAvailable] before this.
     *
     * @return If update available return priority (1 -> 5, with 5 as high priority); Returns -1 if update not available
     */
    val updatePriority: Int
        get() = -1

    fun shouldStartImmediateUpdate(): Boolean {
        return false
    }

    suspend fun shouldStartImmediateUpdateFlow(): Boolean {
        return false
    }

    fun startImmediateUpdateFlow(activity: Activity, requestCode: Int) {
        // no-op
    }

    fun resumeUpdateIfStarted(activity: Activity, requestCode: Int) {
        // no-op
    }
}