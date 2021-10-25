package com.thewizrd.shared_resources.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
        val paint = TextPaint().apply {
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                context.resources.displayMetrics
            )
            setTypeface(typeface)
            color = Colors.BLACK
            style = Paint.Style.FILL
        }

        val width = paint.measureText(text, 0, text.length).toInt()
        val textLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(true)
                .setMaxLines(1)
                .build()
        } else {
            StaticLayout(
                text, paint, width,
                Layout.Alignment.ALIGN_CENTER,
                1.0f,
                0f,
                true
            )
        }

        return Rect(0, 0, textLayout.width, textLayout.height)
    }
}