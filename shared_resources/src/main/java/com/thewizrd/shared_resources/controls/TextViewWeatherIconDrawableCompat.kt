package com.thewizrd.shared_resources.controls

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.SettingsManager

class TextViewWeatherIconDrawableCompat : TextViewDrawableCompat {
    private var mWeatherIconStart: String? = null
    private var mWeatherIconEnd: String? = null
    private var mWeatherIconTop: String? = null
    private var mWeatherIconBottom: String? = null

    private var mIconProvider: String? = null
    private var mShouldAnimate = false
    private var mShowAsMonochrome = false
    private var mIconTint: ColorStateList? = null

    private val wim = WeatherIconsManager.getInstance()

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
                        mIconProvider = WeatherIconsProvider.KEY
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

    fun useDefaultIconProvider() {
        this.iconProvider = WeatherIconsProvider.KEY
    }

    private fun updateIconDrawables() {
        val wip = WeatherIconsManager.getProvider(
            iconProvider ?: SettingsManager(context).getIconsProvider()
        )
        if (shouldAnimate && wip is AVDIconsProviderInterface) {
            val drawableStart = weatherIconStart?.let { wip.getAnimatedDrawable(context, it) }
            val drawableEnd = weatherIconEnd?.let { wip.getAnimatedDrawable(context, it) }
            val drawableTop = weatherIconTop?.let { wip.getAnimatedDrawable(context, it) }
            val drawableBottom = weatherIconBottom?.let { wip.getAnimatedDrawable(context, it) }

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

            this.setCompoundDrawablesRelative(
                drawableStart,
                drawableTop,
                drawableEnd,
                drawableBottom
            )
        } else {
            val drawableStart = weatherIconStart?.let {
                ContextCompat.getDrawable(
                    context,
                    wip.getWeatherIconResource(it)
                )
            }
            val drawableEnd = weatherIconEnd?.let {
                ContextCompat.getDrawable(
                    context,
                    wip.getWeatherIconResource(it)
                )
            }
            val drawableTop = weatherIconTop?.let {
                ContextCompat.getDrawable(
                    context,
                    wip.getWeatherIconResource(it)
                )
            }
            val drawableBottom = weatherIconBottom?.let {
                ContextCompat.getDrawable(
                    context,
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