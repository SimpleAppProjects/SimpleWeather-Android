package com.thewizrd.simpleweather.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thewizrd.shared_resources.utils.Logger

class DebugModeReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_DEBUGMODECHANGED = "SimpleWeather.Droid.action.DEBUG_MODE_CHANGED"
        private const val EXTRA_MODE_ENABLED = "SimpleWeather.Droid.extra.MODE_ENABLED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (ACTION_DEBUGMODECHANGED == intent?.action) {
            Logger.enableDebugLogger(context, intent.getBooleanExtra(EXTRA_MODE_ENABLED, false))
        }
    }
}