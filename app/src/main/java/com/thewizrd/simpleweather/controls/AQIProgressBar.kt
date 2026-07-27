package com.thewizrd.simpleweather.controls

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import kotlin.math.max
import kotlin.math.min

class AQIProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var currentConfig: Configuration
    private var viewHeight: Int = 0
    private var viewWidth: Int = 0
    private var bottomTextHeight: Float = 0f
    private val thumbSize: Int

    private val thumbDrawable: MaterialShapeDrawable
    private val trackPaint: Paint
    private val bottomTextPaint: Paint
    private var bottomTextDescent: Int = 0

    private val bottomTextTopMargin = context.dpToPx(8f)

    private val sideLineLength = context.dpToPx(45f) / 3 * 2
    private val backgroundGridWidth = context.dpToPx(45f)

    private var BOTTOM_TEXT_COLOR = Colors.WHITE
    private var THUMB_COLOR = Colors.WHITE

    init {
        this.currentConfig = Configuration(context.resources.configuration)

        thumbDrawable = MaterialShapeDrawable().apply {
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
            fillColor = ColorStateList.valueOf(THUMB_COLOR)
            elevation =
                context.resources.getDimension(com.google.android.material.R.dimen.m3_slider_thumb_elevation)

            thumbSize =
                context.resources.getDimensionPixelSize(com.google.android.material.R.dimen.mtrl_slider_thumb_radius) * 3 / 4

            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, thumbSize / 2f)
                .build()

            setBounds(0, 0, thumbSize, thumbSize)
        }

        trackPaint = Paint()
        trackPaint.style = Paint.Style.STROKE
        trackPaint.isAntiAlias = true
        trackPaint.strokeWidth = context.dpToPx(4f)
        trackPaint.strokeCap = Paint.Cap.ROUND
        trackPaint.strokeJoin = Paint.Join.ROUND

        bottomTextPaint = Paint()
        bottomTextPaint.isAntiAlias = true
        bottomTextPaint.textSize =
            context.resources.getDimensionPixelSize(R.dimen.forecast_condition_size).toFloat()
        bottomTextPaint.textAlign = Paint.Align.CENTER
        bottomTextPaint.style = Paint.Style.FILL
        bottomTextPaint.color = BOTTOM_TEXT_COLOR

        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.AQIProgressBar).use {
                if (it.hasValue(R.styleable.AQIProgressBar_android_progress)) {
                    progress = it.getInteger(R.styleable.AQIProgressBar_android_progress, 0)
                }
            }
        }

        updateColors()
    }

    private fun setTextColor(@ColorInt color: Int) {
        if (BOTTOM_TEXT_COLOR != color) {
            BOTTOM_TEXT_COLOR = color
            bottomTextPaint.color = BOTTOM_TEXT_COLOR
            invalidate()
        }
    }

    private fun setThumbColor(@ColorInt color: Int) {
        if (THUMB_COLOR != color) {
            THUMB_COLOR = color
            thumbDrawable.fillColor = ColorStateList.valueOf(THUMB_COLOR)
            invalidate()
        }
    }

    var progress: Int = 0

    override fun onDraw(canvas: Canvas) {
        drawLabels(canvas)
        drawTrack(canvas)
        drawThumb(canvas)
    }

    private fun drawLabels(canvas: Canvas) {
        // Draw bottom text
        val y = viewHeight - bottomTextDescent - trackPaint.strokeWidth

        COLOR_MAP.forEachIndexed { index, pair ->
            val text = pair.first.first.toString()
            val bounds = bottomTextPaint.measureText(text)

            if (index == 0) {
                canvas.drawText(text, bounds / 2, y, bottomTextPaint)
            } else {
                canvas.drawText(
                    (pair.first.first - 1).toString(),
                    viewWidth.toFloat() * (pair.first.first / 500f),
                    y,
                    bottomTextPaint
                )
            }

            if (index == COLOR_MAP.size - 1) {
                canvas.drawText(
                    pair.first.last.toString(),
                    viewWidth.toFloat() - bounds / 2,
                    y,
                    bottomTextPaint
                )
            }
        }
    }

    private fun drawThumb(canvas: Canvas) {
        val x = viewWidth * (progress / 500f)
        val y = trackPaint.strokeWidth * 1.5f

        val bounds = Rect(0, 0, thumbSize, thumbSize)
        thumbDrawable.bounds = bounds

        canvas.save()
        canvas.translate(x - bounds.width() / 2f, y - bounds.height() / 2f)
        thumbDrawable.draw(canvas)
        canvas.restore()
    }

    private fun drawTrack(canvas: Canvas) {
        COLOR_MAP.asReversed().forEach { pair ->
            trackPaint.color = pair.second
            val pct = (pair.first.last / 500f)
            canvas.drawRoundRect(
                trackPaint.strokeWidth / 2f,
                trackPaint.strokeWidth * 1.5f,
                viewWidth.toFloat() * pct - trackPaint.strokeWidth / 2f,
                trackPaint.strokeWidth * 1.5f,
                trackPaint.strokeWidth / 2f,
                trackPaint.strokeWidth / 2f,
                trackPaint
            )
        }
    }

    private fun updateColors() {
        val systemNightMode: Int = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES

        setTextColor(if (isNightMode) Colors.WHITE else Colors.BLACK)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        viewWidth = measureWidth(widthMeasureSpec)
        viewHeight = measureHeight(heightMeasureSpec)
        setMeasuredDimension(viewWidth, viewHeight)
    }

    private fun measureWidth(measureSpec: Int): Int {
        val MIN_HORIZONTAL_GRID_NUM = 2
        val preferred = (backgroundGridWidth * MIN_HORIZONTAL_GRID_NUM + sideLineLength * 2).toInt()
        return getMeasurement(measureSpec, preferred)
    }

    private fun measureHeight(measureSpec: Int): Int {
        val preferred = max(
            minimumHeight,
            (bottomTextTopMargin + bottomTextHeight + bottomTextDescent).times(2)
                .plus(trackPaint.strokeWidth * 3).toInt()
        )
        return getMeasurement(measureSpec, preferred)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        currentConfig = Configuration(newConfig)
        updateColors()

        invalidate()
    }

    private fun getMeasurement(measureSpec: Int, preferred: Int): Int {
        val specSize = MeasureSpec.getSize(measureSpec)
        val measurement = when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> min(preferred.toDouble(), specSize.toDouble()).toInt()
            else -> preferred
        }
        return measurement
    }

    companion object {
        private val COLOR_MAP = listOf(
            0 until 51 to Colors.LIMEGREEN,
            51 until 101 to Color.rgb(0xff, 0xde, 0x33),
            101 until 151 to Color.rgb(0xff, 0x99, 0x33),
            151 until 201 to Color.rgb(0xcc, 0x00, 0x33),
            201 until 301 to Color.rgb(0xaa, 0x00, 0xff),
            301 until 501 to Color.rgb(0xbd, 0x00, 0x35)
        )
    }
}