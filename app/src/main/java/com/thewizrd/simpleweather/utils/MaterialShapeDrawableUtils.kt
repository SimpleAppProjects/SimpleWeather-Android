package com.thewizrd.simpleweather.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.material.elevation.ElevationOverlayProvider
import com.google.android.material.shape.MaterialShapeDrawable

object MaterialShapeDrawableUtils {
    @ColorInt
    @JvmStatic
    @JvmOverloads
    fun MaterialShapeDrawable.getTintColor(
        context: Context,
        @ColorInt backgroundColor: Int? = null
    ): Int {
        if (this.tintList == null) {
            val paintColor = backgroundColor ?: (this.fillColor?.defaultColor ?: 0)
            val elevation = this.z + this.parentAbsoluteElevation
            return if (this.isElevationOverlayInitialized) {
                ElevationOverlayProvider(context).compositeOverlayIfNeeded(paintColor, elevation)
            } else {
                paintColor
            }
        } else {
            val tintColor = this.tintList!!.getColorForState(state, Color.TRANSPARENT)
            return if (this.isElevationOverlayInitialized) {
                ElevationOverlayProvider(context).compositeOverlayIfNeeded(tintColor, elevation)
            } else {
                tintColor
            }
        }
    }
}