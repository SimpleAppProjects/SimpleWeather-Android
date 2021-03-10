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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED) {
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
    }
}