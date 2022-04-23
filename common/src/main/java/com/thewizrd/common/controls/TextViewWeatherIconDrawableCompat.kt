package com.thewizrd.common.controls

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RotateDrawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.SettingsManager

class TextViewWeatherIconDrawableCompat : TextViewDrawableCompat {
    private var mWeatherIconStart: String? = null
    private var mWeatherIconEnd: String? = null
    private var mWeatherIconTop: String? = null
    private var mWeatherIconBottom: String? = null

    private var mIconRotation: Int = 0

    private var mIconProvider: String? = null
    private var mShouldAnimate = false
    private var mShowAsMonochrome = false
    private var mIconTint: ColorStateList? = null
    private var mForceDarkMode: Boolean = false

    private val wim: WeatherIconsManager
        get() = sharedDeps.weatherIconsManager

    @SuppressLint("CustomViewStyleable")
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        if (attrs != null) {
            val a =
                context.obtainStyledAttributes(attrs, R.styleable.TextViewWeatherIconDrawableCompat)

            try {
                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_iconProvider)) {
                    mIconProvider =
                        a.getString(R.styleable.TextViewWeatherIconDrawableCompat_iconProvider)
                }

                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_animate)) {
                    mShouldAnimate =
                        a.getBoolean(R.styleable.TextViewWeatherIconDrawableCompat_animate, false)
                }

                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_showAsMonochrome)) {
                    mShowAsMonochrome = a.getBoolean(
                        R.styleable.TextViewWeatherIconDrawableCompat_showAsMonochrome,
                        false
                    )
                }

                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_useDefaultIconProvider)) {
                    val useDefault = a.getBoolean(
                        R.styleable.TextViewWeatherIconDrawableCompat_useDefaultIconProvider,
                        false
                    )
                    if (useDefault) {
                        mIconProvider = WeatherIconsEFProvider.KEY
                    }
                }

                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconStart)) {
                    mWeatherIconStart =
                        a.getString(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconStart)
                }
                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconEnd)) {
                    mWeatherIconEnd =
                        a.getString(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconEnd)
                }
                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconTop)) {
                    mWeatherIconTop =
                        a.getString(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconTop)
                }
                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconBottom)) {
                    mWeatherIconBottom =
                        a.getString(R.styleable.TextViewWeatherIconDrawableCompat_weatherIconBottom)
                }

                if (a.hasValue(R.styleable.TextViewWeatherIconDrawableCompat_iconRotation)) {
                    mIconRotation =
                        a.getInteger(R.styleable.TextViewWeatherIconDrawableCompat_iconRotation, 0)
                }

                if (a.hasValue(R.styleable.IconControl_forceDarkMode)) {
                    mForceDarkMode = a.getBoolean(R.styleable.IconControl_forceDarkMode, false)
                }
            } finally {
                a.recycle()
            }
        }

        updateIconDrawables()
        updateIconTint()
    }

    @SuppressLint("RestrictedApi")
    override fun setSupportCompoundDrawablesTintList(tintList: ColorStateList?) {
        super.setSupportCompoundDrawablesTintList(tintList)
        mIconTint = tintList
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun setCompoundDrawableTintList(tint: ColorStateList?) {
        super.setCompoundDrawableTintList(tint)
        mIconTint = tint
    }

    var weatherIconStart: String?
        get() = mWeatherIconStart
        set(icon) {
            mWeatherIconStart = icon
            updateIconDrawables()
        }
    var weatherIconEnd: String?
        get() = mWeatherIconEnd
        set(icon) {
            mWeatherIconEnd = icon
            updateIconDrawables()
        }
    var weatherIconTop: String?
        get() = mWeatherIconTop
        set(icon) {
            mWeatherIconTop = icon
            updateIconDrawables()
        }
    var weatherIconBottom: String?
        get() = mWeatherIconBottom
        set(icon) {
            mWeatherIconBottom = icon
            updateIconDrawables()
        }

    var iconProvider: String?
        get() = mIconProvider
        set(provider) {
            mIconProvider = provider
            updateIconDrawables()
        }

    var showAsMonochrome: Boolean
        get() = mShowAsMonochrome
        set(value) {
            mShowAsMonochrome = value
            updateIconTint()
        }

    var shouldAnimate: Boolean
        get() = mShouldAnimate
        set(value) {
            mShouldAnimate = value
            updateIconDrawables()
        }

    var iconRotation: Int
        get() = mIconRotation
        set(value) {
            mIconRotation = value
            updateIconDrawables()
        }

    var forceDarkMode: Boolean
        get() = mForceDarkMode
        set(value) {
            mForceDarkMode = value
            updateIconDrawables()
        }

    fun useDefaultIconProvider() {
        this.iconProvider = WeatherIconsEFProvider.KEY
    }

    private fun updateIconDrawables() {
        val iconCtx = if (mForceDarkMode) {
            context.getThemeContextOverride(false)
        } else {
            context
        }

        val wip = wim.getIconProvider(
            iconProvider ?: SettingsManager(context).getIconsProvider()
        )

        if (shouldAnimate && wip is AVDIconsProviderInterface) {
            var drawableStart = weatherIconStart?.let { wip.getAnimatedDrawable(iconCtx, it) }
            var drawableEnd = weatherIconEnd?.let { wip.getAnimatedDrawable(iconCtx, it) }
            var drawableTop = weatherIconTop?.let { wip.getAnimatedDrawable(iconCtx, it) }
            var drawableBottom = weatherIconBottom?.let { wip.getAnimatedDrawable(iconCtx, it) }

            if (drawableStart is Animatable && !drawableStart.isRunning) {
                drawableStart.start()
            }
            if (drawableEnd is Animatable && !drawableEnd.isRunning) {
                drawableEnd.start()
            }
            if (drawableTop is Animatable && !drawableTop.isRunning) {
                drawableTop.start()
            }
            if (drawableBottom is Animatable && !drawableBottom.isRunning) {
                drawableBottom.start()
            }

            if (mIconRotation != 0) {
                drawableStart = drawableStart?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableTop = drawableTop?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableEnd = drawableEnd?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableBottom = drawableBottom?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }
            }

            this.setCompoundDrawablesRelative(
                drawableStart,
                drawableTop,
                drawableEnd,
                drawableBottom
            )
        } else {
            var drawableStart = weatherIconStart?.let {
                ContextCompat.getDrawable(
                    iconCtx,
                    wip.getWeatherIconResource(it)
                )
            }
            var drawableEnd = weatherIconEnd?.let {
                ContextCompat.getDrawable(
                    iconCtx,
                    wip.getWeatherIconResource(it)
                )
            }
            var drawableTop = weatherIconTop?.let {
                ContextCompat.getDrawable(
                    iconCtx,
                    wip.getWeatherIconResource(it)
                )
            }
            var drawableBottom = weatherIconBottom?.let {
                ContextCompat.getDrawable(
                    iconCtx,
                    wip.getWeatherIconResource(it)
                )
            }

            if (drawableStart is Animatable && !drawableStart.isRunning) {
                drawableStart.start()
            }
            if (drawableEnd is Animatable && !drawableEnd.isRunning) {
                drawableEnd.start()
            }
            if (drawableTop is Animatable && !drawableTop.isRunning) {
                drawableTop.start()
            }
            if (drawableBottom is Animatable && !drawableBottom.isRunning) {
                drawableBottom.start()
            }

            if (mIconRotation != 0) {
                drawableStart = drawableStart?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableTop = drawableTop?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableEnd = drawableEnd?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }

                drawableBottom = drawableBottom?.let {
                    RotateDrawable().apply {
                        fromDegrees = mIconRotation.toFloat()
                        toDegrees = mIconRotation.toFloat()
                        drawable = it
                        level = 10000
                    }.apply {
                        level = 0
                    }
                }
            }

            this.setCompoundDrawablesRelative(
                drawableStart,
                drawableTop,
                drawableEnd,
                drawableBottom
            )
        }
    }

    private fun updateIconTint() {
        if (showAsMonochrome || wim.isFontIcon) {
            TextViewCompat.setCompoundDrawableTintList(this, mIconTint)
        } else {
            val tint = mIconTint
            TextViewCompat.setCompoundDrawableTintList(this, null)
            mIconTint = tint
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val drawables = this.compoundDrawables
        drawables.forEach { it: Drawable? ->
            if (it is Animatable && !it.isRunning) {
                it.start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        val drawables = this.compoundDrawables
        drawables.forEach { it: Drawable? ->
            if (it is Animatable) {
                it.stop()
            }
        }
    }
}