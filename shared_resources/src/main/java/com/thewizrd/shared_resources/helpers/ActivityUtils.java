package com.thewizrd.shared_resources.helpers;

import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;

import com.thewizrd.shared_resources.utils.Colors;

public final class ActivityUtils {
    public static void setTransparentWindow(@NonNull Window window, @ColorInt int color) {
        setTransparentWindow(window, color, color, color, true);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int navBarColor) {
        setTransparentWindow(window, backgroundColor, statusBarColor, navBarColor, true);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int navBarColor, boolean setColors) {
        // Make full transparent statusBar
        boolean isLightNavBar =
                (navBarColor != Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, ColorUtils.setAlphaComponent(navBarColor, 0xFF)) < 4.5f) ||
                        (navBarColor == Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, backgroundColor) < 4.5f);
        boolean navBarProtected = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) || !isLightNavBar;

        boolean isLightStatusBar =
                (statusBarColor != Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, ColorUtils.setAlphaComponent(statusBarColor, 0xFF)) < 4.5f) ||
                        (statusBarColor == Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, backgroundColor) < 4.5f);
        boolean statBarProtected = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) || !isLightStatusBar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setLightStatusBar(window, setColors && isLightStatusBar);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setLightNavBar(window, setColors && isLightNavBar);
        }

        window.setStatusBarColor((setColors ?
                (statBarProtected ? statusBarColor : ColorUtils.blendARGB(statusBarColor, Colors.BLACK, 0.25f))
                : Colors.TRANSPARENT));
        window.setNavigationBarColor((setColors ?
                (navBarProtected ? navBarColor : ColorUtils.blendARGB(navBarColor, Colors.BLACK, 0.25f))
                : Colors.TRANSPARENT));
    }

    public static void setFullScreen(@NonNull Window window, boolean fullScreen) {
        WindowCompat.setDecorFitsSystemWindows(window, !fullScreen);
    }

    public static void setStatusBarColor(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor) {
        setStatusBarColor(window, backgroundColor, statusBarColor, true);
    }

    private static void setStatusBarColor(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, boolean setColors) {
        boolean isLightStatusBar =
                (statusBarColor != Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, ColorUtils.setAlphaComponent(statusBarColor, 0xFF)) < 4.5f) ||
                        (statusBarColor == Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, backgroundColor) < 4.5f);
        boolean statBarProtected = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) || !isLightStatusBar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (setColors && isLightStatusBar) {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility()
                                & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        window.setStatusBarColor((setColors ?
                (statBarProtected ? statusBarColor : ColorUtils.blendARGB(statusBarColor, Colors.BLACK, 0.25f))
                : Colors.TRANSPARENT));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setLightStatusBar(@NonNull Window window, boolean setLight) {
        if (setLight) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setLightNavBar(@NonNull Window window, boolean setLight) {
        if (setLight) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }
}
