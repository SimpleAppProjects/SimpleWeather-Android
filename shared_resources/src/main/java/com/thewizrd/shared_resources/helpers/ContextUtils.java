package com.thewizrd.shared_resources.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public final class ContextUtils {
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

    public static Context getThemeContextOverride(@NonNull Context context, boolean isLight) {
        Configuration oldConfig = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration(oldConfig);

        newConfig.uiMode = (isLight ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES)
                | (newConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);

        return context.createConfigurationContext(newConfig);
    }
}
