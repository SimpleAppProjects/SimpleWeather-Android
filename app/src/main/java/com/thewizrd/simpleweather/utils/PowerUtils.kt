package com.thewizrd.simpleweather.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.App

class PowerUtils {
    companion object {
        const val KEY_USE_FOREGROUNDSERVICE = "key_use_foregroundservice"
        const val KEY_REQUESTIGNOREBATOPTS = "key_request_ignorebatopts"

        var useForegroundService: Boolean
            get() {
                return App.instance.preferences.getBoolean(KEY_USE_FOREGROUNDSERVICE, false)
            }
            set(value) {
                App.instance.preferences.edit().also {
                    it.putBoolean(KEY_USE_FOREGROUNDSERVICE, value)
                    it.commit()
                }
            }

        @JvmStatic
        fun checkBackgroundOptimizationPermission(context: Context): Boolean {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }

            return false
        }

        @RequiresApi(Build.VERSION_CODES.M)
        @JvmStatic
        fun isBackgroundOptimizationDisabled(context: Context): Boolean {
            val pwrMan = context.getSystemService(PowerManager::class.java)
            return pwrMan.isIgnoringBatteryOptimizations(context.packageName)
        }

        @SuppressLint("BatteryLife")
        @RequiresApi(Build.VERSION_CODES.M)
        @JvmStatic
        fun canStartIgnoreBatteryOptActivity(context: Context): Boolean {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).also {
                it.data = Uri.Builder().scheme("package").authority(context.packageName).build()
            }
            return intent.resolveActivity(context.packageManager) != null
        }

        @SuppressLint("BatteryLife")
        @RequiresApi(Build.VERSION_CODES.M)
        @JvmStatic
        fun startIgnoreBatteryOptActivity(context: Context) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).also {
                it.data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun startForegroundService(context: Context, intent: Intent, onBoot: Boolean = false) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || App.instance.appState == AppState.FOREGROUND || onBoot) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                Logger.writeLine(
                    Log.INFO,
                    "Foreground service not started (action: ${intent.action};" +
                            "SDK: ${Build.VERSION.SDK_INT}, AppState: ${App.instance.appState}, onBoot: $onBoot"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    LocalBroadcastManager.getInstance(context.applicationContext)
                        .sendBroadcast(intent)
                }
            }
        }
    }
}