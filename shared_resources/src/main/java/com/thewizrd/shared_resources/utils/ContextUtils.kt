package com.thewizrd.shared_resources.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

object ContextUtils {
    @JvmStatic
    fun Context.dpToPx(valueInDp: Float): Float {
        val metrics = this.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
    }

    @JvmStatic
    fun Context.isLargeTablet(): Boolean {
        return (this.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    @JvmStatic
    fun Context.isXLargeTablet(): Boolean {
        return (this.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    @JvmStatic
    fun Context.isSmallestWidth(swdp: Int): Boolean {
        return this.resources.configuration.smallestScreenWidthDp >= swdp
    }

    @JvmStatic
    fun Context.getOrientation(): Int {
        return this.resources.configuration.orientation
    }

    @JvmStatic
    fun Context.getAttrDimension(@AttrRes resId: Int): Int {
        val value = TypedValue()
        this.theme.resolveAttribute(resId, value, true)
        return TypedValue.complexToDimensionPixelSize(
            value.data,
            this.resources.displayMetrics
        )
    }

    @JvmStatic
    fun Context.getAttrValue(@AttrRes resId: Int): Int {
        val value = TypedValue()
        this.theme.resolveAttribute(resId, value, true)
        return value.data
    }

    @JvmStatic
    @ColorInt
    fun Context.getAttrColor(@AttrRes resId: Int): Int {
        val array = this.theme.obtainStyledAttributes(intArrayOf(resId))
        @ColorInt val color = array.getColor(0, 0)
        array.recycle()
        return color
    }

    @JvmStatic
    fun Context.getAttrColorStateList(@AttrRes resId: Int): ColorStateList? {
        val array = this.theme.obtainStyledAttributes(intArrayOf(resId))
        var color: ColorStateList? = null
        color = try {
            array.getColorStateList(0)
        } finally {
            array.recycle()
        }
        return color
    }

    @JvmStatic
    fun Context.getAttrDrawable(@AttrRes resId: Int): Drawable? {
        val array = this.theme.obtainStyledAttributes(intArrayOf(resId))
        val drawable = array.getDrawable(0)
        array.recycle()
        return drawable
    }

    @JvmStatic
    @AnyRes
    fun Context.getAttrResourceId(@AttrRes resId: Int): Int {
        val array = this.theme.obtainStyledAttributes(intArrayOf(resId))
        val resourceId = array.getResourceId(0, 0)
        array.recycle()
        return resourceId
    }

    @JvmStatic
    fun Context.verifyActivityInfo(componentName: ComponentName): Boolean {
        try {
            packageManager.getActivityInfo(componentName, PackageManager.MATCH_DEFAULT_ONLY)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
        }

        return false
    }

    @JvmStatic
    fun Context.getThemeContextOverride(isLight: Boolean): Context {
        val oldConfig = resources.configuration
        val newConfig = Configuration(oldConfig)

        newConfig.uiMode = (
                (if (isLight) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES)
                        or (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
                )

        return createConfigurationContext(newConfig)
    }
}