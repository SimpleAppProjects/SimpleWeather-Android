package com.thewizrd.shared_resources.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue

object TextUtils {
    fun CharSequence.getTextBounds(
        context: Context,
        textSizeSp: Float,
        typeface: Typeface = Typeface.SANS_SERIF
    ): Rect {
        return getTextBounds(context, this, textSizeSp, typeface)
    }

    @JvmStatic
    fun getTextBounds(
        context: Context,
        text: CharSequence, textSizeSp: Float,
        typeface: Typeface = Typeface.SANS_SERIF
    ): Rect {
        val paint = Paint().apply {
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                context.resources.displayMetrics
            )
            setTypeface(typeface)
            color = Colors.BLACK
            style = Paint.Style.FILL
        }

        val rect = Rect()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.getTextBounds(text, 0, text.length, rect)
        } else {
            paint.getTextBounds(text.toString(), 0, text.length, rect)
        }

        return rect
    }
}