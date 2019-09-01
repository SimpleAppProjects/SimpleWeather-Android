package com.thewizrd.simpleweather.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;

public class ActivityUtils {
    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public static boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int statusBarColor, @ColorInt int navBarColor) {
        int windowBGColor = Colors.WHITE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window.getDecorView().getBackground() instanceof ColorDrawable) {
            windowBGColor = ((ColorDrawable) window.getDecorView().getBackground()).getColor();
        }

        setTransparentWindow(window, windowBGColor, statusBarColor, navBarColor);
    }

    public static void setTransparentWindow(@NonNull Window window, @ColorInt int backgroundColor, @ColorInt int statusBarColor, @ColorInt int navBarColor) {
        // Make full transparent statusBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            WindowManager.LayoutParams winParams = window.getAttributes();
            if (statusBarColor >= 0)
                winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            else
                winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            if (navBarColor >= 0)
                winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            else
                winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;

            window.setAttributes(winParams);
        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int targetSDK = window.getContext().getApplicationInfo().targetSdkVersion;

            boolean isLightNavBar =
                    ColorsUtils.isSuperLight(navBarColor) || (navBarColor == Colors.TRANSPARENT && ColorsUtils.isSuperLight(backgroundColor));
            boolean navBarProtection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && targetSDK > Build.VERSION_CODES.P) || !isLightNavBar;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isLightNavBar) {
                    window.getDecorView().setSystemUiVisibility(
                            window.getDecorView().getSystemUiVisibility()
                                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            window.getDecorView().getSystemUiVisibility()
                                    & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                }
            }

            window.setStatusBarColor(statusBarColor);
            window.setNavigationBarColor(navBarProtection ? navBarColor : ColorUtils.setAlphaComponent(backgroundColor, 0xF0));
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

    @AnyRes
    public static int getResourceId(@NonNull Context activityContext, @AttrRes int resId) {
        final TypedArray array = activityContext.getTheme().obtainStyledAttributes(new int[]{resId});
        int resourceId = array.getResourceId(0, 0);
        array.recycle();

        return resourceId;
    }
}
