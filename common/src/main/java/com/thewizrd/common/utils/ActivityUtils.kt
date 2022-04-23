package com.thewizrd.common.utils

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ActivityUtils {
    fun Window.setTransparentWindow(@ColorInt color: Int) {
        setTransparentWindow(color, color, color, true)
    }

    fun Window.setTransparentWindow(
        @ColorInt backgroundColor: Int,
        @ColorInt statusBarColor: Int,
        @ColorInt navBarColor: Int
    ) {
        setTransparentWindow(backgroundColor, statusBarColor, navBarColor, true)
    }

    fun Window.setTransparentWindow(
        @ColorInt backgroundColor: Int,
        @ColorInt statusBarColor: Int,
        @ColorInt navBarColor: Int,
        setColors: Boolean
    ) {
        // Make full transparent statusBar
        val isLightNavBar =
            navBarColor != Color.TRANSPARENT && ColorUtils.calculateContrast(
                Color.WHITE,
                ColorUtils.setAlphaComponent(navBarColor, 0xFF)
            ) < 4.5f ||
                    navBarColor == Color.TRANSPARENT && ColorUtils.calculateContrast(
                Color.WHITE,
                backgroundColor
            ) < 4.5f
        val navBarProtected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || !isLightNavBar

        val isLightStatusBar = statusBarColor != Color.TRANSPARENT && ColorUtils.calculateContrast(
            Color.WHITE,
            ColorUtils.setAlphaComponent(statusBarColor, 0xFF)
        ) < 4.5f ||
                statusBarColor == Color.TRANSPARENT && ColorUtils.calculateContrast(
            Color.WHITE,
            backgroundColor
        ) < 4.5f
        val statBarProtected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || !isLightStatusBar

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setLightStatusBar(setColors && isLightStatusBar)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setLightNavBar(setColors && isLightNavBar)
        }

        this.statusBarColor = if (setColors) {
            if (statBarProtected) statusBarColor else ColorUtils.blendARGB(
                statusBarColor,
                Color.BLACK,
                0.25f
            )
        } else {
            Color.TRANSPARENT
        }
        this.navigationBarColor = if (setColors) {
            if (navBarProtected) navBarColor else ColorUtils.blendARGB(
                navBarColor,
                Color.BLACK,
                0.25f
            )
        } else {
            Color.TRANSPARENT
        }
    }

    fun Window.setFullScreen(fullScreen: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(this, !fullScreen)
    }

    fun Window.setStatusBarColor(
        @ColorInt backgroundColor: Int,
        @ColorInt statusBarColor: Int
    ) {
        setStatusBarColor(backgroundColor, statusBarColor, true)
    }

    private fun Window.setStatusBarColor(
        @ColorInt backgroundColor: Int,
        @ColorInt statusBarColor: Int,
        setColors: Boolean
    ) {
        val isLightStatusBar = statusBarColor != Color.TRANSPARENT && ColorUtils.calculateContrast(
            Color.WHITE, ColorUtils.setAlphaComponent(statusBarColor, 0xFF)
        ) < 4.5f ||
                statusBarColor == Color.TRANSPARENT && ColorUtils.calculateContrast(
            Color.WHITE,
            backgroundColor
        ) < 4.5f
        val statBarProtected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || !isLightStatusBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (setColors && isLightStatusBar) {
                decorView.systemUiVisibility = (
                        decorView.systemUiVisibility
                                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            } else {
                decorView.systemUiVisibility = (
                        decorView.systemUiVisibility
                                and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv())
            }
        }
        this.statusBarColor = if (setColors) {
            if (statBarProtected) statusBarColor else ColorUtils.blendARGB(
                statusBarColor,
                Color.BLACK,
                0.25f
            )
        } else {
            Color.TRANSPARENT
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun Window.setLightStatusBar(setLight: Boolean) {
        if (setLight) {
            decorView.systemUiVisibility = (
                    decorView.systemUiVisibility
                            or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        } else {
            decorView.systemUiVisibility = (
                    decorView.systemUiVisibility
                            and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv())
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun Window.setLightNavBar(setLight: Boolean) {
        if (setLight) {
            decorView.systemUiVisibility = (
                    decorView.systemUiVisibility
                            or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        } else {
            decorView.systemUiVisibility = (
                    decorView.systemUiVisibility
                            and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv())
        }
    }

    fun ComponentActivity.showToast(@StringRes resId: Int, duration: Int) {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            Toast.makeText(this@showToast, resId, duration).show()
        }
    }

    fun ComponentActivity.showToast(message: CharSequence?, duration: Int) {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            Toast.makeText(this@showToast, message, duration).show()
        }
    }
}