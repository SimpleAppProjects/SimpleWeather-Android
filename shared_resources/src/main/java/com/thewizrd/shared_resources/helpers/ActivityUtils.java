package com.thewizrd.shared_resources.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.thewizrd.shared_resources.utils.Colors;

public class ActivityUtils {
    public static float dpToPx(@NonNull Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static boolean isLargeTablet(@NonNull Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isXLargeTablet(@NonNull Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static boolean isSmallestWidth(@NonNull Context context, int swdp) {
        return (context.getResources().getConfiguration().smallestScreenWidthDp) >= swdp;
    }

    public static int getOrientation(@NonNull Context context) {
        return (context.getResources().getConfiguration().orientation);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int color) {
        setTransparentWindow(window, color, color, color, true);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int navBarColor) {
        setTransparentWindow(window, backgroundColor, statusBarColor, navBarColor, true);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int navBarColor, boolean setColors) {
        // Make full transparent statusBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean isLightNavBar =
                    (navBarColor != Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, ColorUtils.setAlphaComponent(navBarColor, 0xFF)) < 4.5f) ||
                            (navBarColor == Colors.TRANSPARENT && ColorUtils.calculateContrast(Colors.WHITE, backgroundColor) < 4.5f);
            boolean navBarProtected = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) || !isLightNavBar;

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (setColors && isLightNavBar) {
                    window.getDecorView().setSystemUiVisibility(
                            window.getDecorView().getSystemUiVisibility()
                                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            window.getDecorView().getSystemUiVisibility()
                                    & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                }
            }

            window.setStatusBarColor((setColors ?
                    (statBarProtected ?
                            statusBarColor == Colors.TRANSPARENT ? ColorUtils.setAlphaComponent(backgroundColor, 0xB3) : statusBarColor :
                            ColorUtils.blendARGB(statusBarColor, Colors.BLACK, 0.25f))
                    : Colors.TRANSPARENT));
            window.setNavigationBarColor((setColors ?
                    (navBarProtected ? navBarColor : ColorUtils.blendARGB(navBarColor, Colors.BLACK, 0.25f))
                    : Colors.TRANSPARENT));
        }
    }

    public static int getAttrDimension(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedValue value = new TypedValue();
        activityContext.getTheme().resolveAttribute(resId, value, true);

        return TypedValue.complexToDimensionPixelSize(value.data, activityContext.getResources().getDisplayMetrics());
    }

    public static int getAttrValue(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedValue value = new TypedValue();
        activityContext.getTheme().resolveAttribute(resId, value, true);

        return value.data;
    }

    @ColorInt
    public static int getColor(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedArray array = activityContext.getTheme().obtainStyledAttributes(new int[]{resId});
        @ColorInt int color = array.getColor(0, 0);
        array.recycle();

        return color;
    }

    public static ColorStateList getColorStateList(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedArray array = activityContext.getTheme().obtainStyledAttributes(new int[]{resId});
        ColorStateList color = array.getColorStateList(0);
        array.recycle();

        return color;
    }

    @AnyRes
    public static int getResourceId(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedArray array = activityContext.getTheme().obtainStyledAttributes(new int[]{resId});
        int resourceId = array.getResourceId(0, 0);
        array.recycle();

        return resourceId;
    }

    public static boolean verifyActivityInfo(@NonNull Context context, @NonNull ComponentName componentName) {
        try {
            context.getPackageManager().getActivityInfo(componentName, PackageManager.MATCH_DEFAULT_ONLY);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return false;
    }
}
