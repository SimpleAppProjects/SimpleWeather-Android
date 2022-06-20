package com.thewizrd.common.controls

import android.graphics.drawable.Drawable
import android.graphics.drawable.RotateDrawable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object TextViewDrawableCompatUtils {
    @JvmStatic
    fun Drawable.wrapInRotateDrawableIfNeeded(rotation: Int): Drawable {
        if (rotation == 0 && this !is RotateDrawable) {
            return this
        }

        if (this is RotateDrawable) {
            return this.apply {
                fromDegrees = rotation.toFloat()
                toDegrees = rotation.toFloat()
                level = 10000
            }.apply {
                level = 0
            }
        } else {
            return RotateDrawable().apply {
                fromDegrees = rotation.toFloat()
                toDegrees = rotation.toFloat()
                drawable = this@wrapInRotateDrawableIfNeeded
                level = 10000
            }.apply {
                level = 0
            }
        }
    }
}