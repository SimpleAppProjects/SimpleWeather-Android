package com.thewizrd.common.controls

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.designer.initializeDependencies
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsEFProvider
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.SettingsManager

class IconControl : AppCompatImageView {
    private var mWeatherIcon: String? = null
    private var mIconProvider: String? = null
    private var mShouldAnimate = false
    private var mShowAsMonochrome = false
    private var mIconTint: ColorStateList? = null
    private var mForceDarkMode: Boolean = false

    private val wim: WeatherIconsManager
        get() = sharedDeps.weatherIconsManager

    private var mIconChangedListener: OnIconChangedListener? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.IconControl)

            try {
                if (a.hasValue(R.styleable.IconControl_weatherIcon)) {
                    mWeatherIcon = a.getString(R.styleable.IconControl_weatherIcon)
                }

                if (a.hasValue(R.styleable.IconControl_iconProvider)) {
                    mIconProvider = a.getString(R.styleable.IconControl_iconProvider)
                }

                if (a.hasValue(R.styleable.IconControl_animate)) {
                    mShouldAnimate = a.getBoolean(R.styleable.IconControl_animate, false)
                }

                if (a.hasValue(R.styleable.IconControl_showAsMonochrome)) {
                    mShowAsMonochrome =
                        a.getBoolean(R.styleable.IconControl_showAsMonochrome, false)
                }

                if (a.hasValue(R.styleable.IconControl_useDefaultIconProvider)) {
                    val useDefault =
                        a.getBoolean(R.styleable.IconControl_useDefaultIconProvider, false)
                    if (useDefault) {
                        mIconProvider = WeatherIconsEFProvider.KEY
                    }
                }

                if (a.hasValue(R.styleable.IconControl_forceDarkMode)) {
                    mForceDarkMode = a.getBoolean(R.styleable.IconControl_forceDarkMode, false)
                }
            } finally {
                a.recycle()
            }
        }

        updateIconDrawable()
        updateIconTint()
    }

    init {
        if (isInEditMode) {
            context.initializeDependencies()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setSupportImageTintList(tint: ColorStateList?) {
        super.setSupportImageTintList(tint)
        mIconTint = tint
    }

    override fun setImageTintList(tint: ColorStateList?) {
        super.setImageTintList(tint)
        mIconTint = tint
    }

    var weatherIcon: String?
        get() = mWeatherIcon
        set(icon) {
            mWeatherIcon = icon
            updateIconDrawable()
        }

    var iconProvider: String?
        get() = mIconProvider
        set(provider) {
            mIconProvider = provider
            updateIconDrawable()
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
            updateIconDrawable()
        }

    var forceDarkMode: Boolean
        get() = mForceDarkMode
        set(value) {
            mForceDarkMode = value
            updateIconDrawable()
        }

    fun useDefaultIconProvider() {
        this.iconProvider = WeatherIconsEFProvider.KEY
    }

    private fun updateIconDrawable() {
        val iconCtx = if (mForceDarkMode) {
            context.getThemeContextOverride(false)
        } else {
            context
        }

        val wip = wim.getIconProvider(
            iconProvider ?: SettingsManager(context).getIconsProvider()
        )

        if (shouldAnimate && wip is AVDIconsProviderInterface) {
            val drawable = wip.getAnimatedDrawable(iconCtx, weatherIcon ?: WeatherIcons.NA)
            this.setImageDrawable(drawable)
            if (drawable is Animatable && !drawable.isRunning) {
                drawable.start()
            }

            this.mIconChangedListener?.onIconChanged(this)
        } else {
            this.setImageDrawable(mWeatherIcon?.let {
                ContextCompat.getDrawable(
                    iconCtx,
                    wip.getWeatherIconResource(it)
                )
            })
            val drawable = this.drawable
            if (shouldAnimate && drawable is Animatable && !drawable.isRunning) {
                drawable.start()
            }

            this.mIconChangedListener?.onIconChanged(this)
        }
    }

    private fun updateIconTint() {
        if (showAsMonochrome || wim.isFontIcon) {
            ImageViewCompat.setImageTintList(this, mIconTint)
        } else {
            val tint = mIconTint
            ImageViewCompat.setImageTintList(this, null)
            mIconTint = tint
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val drawable = this.drawable
        if (drawable is Animatable && !drawable.isRunning) {
            drawable.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        val drawable = this.drawable
        if (drawable is Animatable) {
            drawable.stop()
        }
    }

    fun setOnIconChangedListener(listener: OnIconChangedListener?) {
        this.mIconChangedListener = listener
    }

    public interface OnIconChangedListener {
        fun onIconChanged(view: IconControl)
    }
}