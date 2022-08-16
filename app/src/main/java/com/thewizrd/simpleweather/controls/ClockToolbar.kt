package com.thewizrd.simpleweather.controls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.ViewDebug
import android.widget.TextClock
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.MaterialToolbar
import com.ibm.icu.text.DateFormat
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import com.thewizrd.simpleweather.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A toolbar that has the time as the toolbar subtitle
 *
 * Based on [TextClock]
 */
class ClockToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.toolbarStyle
) : MaterialToolbar(context, attrs, defStyleAttr) {
    private var mFormat12: CharSequence? = null
    private var mFormat24: CharSequence? = null
    private var mDescFormat12: CharSequence? = null
    private var mDescFormat24: CharSequence? = null

    @ViewDebug.ExportedProperty
    private var mFormat: CharSequence? = null
    private lateinit var mFormatter: DateTimeFormatter

    @ViewDebug.ExportedProperty
    private var mHasSeconds = false

    private var mDescFormat: CharSequence? = null
    private lateinit var mDescFormatter: DateTimeFormatter

    private var mRegistered = false
    private var mShouldRunTicker = false

    private lateinit var mTime: ZonedDateTime
    private var mTimeZone: String? = null

    private var mFormatChangeObserver: ContentObserver? = null

    // Used by tests to stop time change events from triggering the text update
    private var mStopTicking = false

    private inner class FormatChangeObserver(handler: Handler?) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            chooseFormat()
            onTimeChanged()
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            chooseFormat()
            onTimeChanged()
        }
    }

    private val mIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mStopTicking) {
                return  // Test disabled the clock ticks
            }
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED == intent.action) {
                val timeZone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    intent.getStringExtra(Intent.EXTRA_TIMEZONE)
                } else {
                    intent.getStringExtra("time-zone")
                }
                createTime(timeZone)
            } else if (!mShouldRunTicker && (Intent.ACTION_TIME_TICK == intent.action || Intent.ACTION_TIME_CHANGED == intent.action)) {
                return
            }
            onTimeChanged()
        }
    }

    private val mTicker: Runnable = object : Runnable {
        override fun run() {
            if (mStopTicking) {
                return  // Test disabled the clock ticks
            }
            onTimeChanged()
            val now = SystemClock.uptimeMillis()
            val next = now + (1000 - now % 1000)
            val handler = handler
            handler?.postAtTime(this, next)
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ClockToolbar)
        ViewCompat.saveAttributeDataForStyleable(
            this, context, R.styleable.ClockToolbar,
            attrs, a, defStyleAttr, R.style.Widget_MaterialComponents_Toolbar
        )

        try {
            mFormat12 = a.getText(R.styleable.ClockToolbar_android_format12Hour)
            mFormat24 = a.getText(R.styleable.ClockToolbar_android_format24Hour)
            mTimeZone = a.getString(R.styleable.ClockToolbar_android_timeZone)
        } finally {
            a.recycle()
        }

        if (mFormat12 == null || mFormat24 == null) {
            if (mFormat12 == null) {
                mFormat12 = DateTimeConstants.CLOCK_FORMAT_12HR_AMPM_TZ
            }
            if (mFormat24 == null) {
                val locale = context.resources.configuration.locale
                mFormat24 = DateTimeUtils.getBestPatternForSkeleton(
                    DateFormat.HOUR24_MINUTE + DateFormat.ABBR_SPECIFIC_TZ, locale
                )
            }
        }

        createTime(mTimeZone)
        chooseFormat()
    }

    private fun createTime(timeZone: String?) {
        mTime = if (timeZone != null) {
            ZonedDateTime.now(ZoneIdCompat.of(timeZone))
        } else {
            ZonedDateTime.now()
        }
    }

    @ViewDebug.ExportedProperty
    fun getFormat12Hour(): CharSequence? {
        return mFormat12
    }

    fun setFormat12Hour(format: CharSequence?) {
        mFormat12 = format

        chooseFormat()
        onTimeChanged()
    }

    fun setContentDescriptionFormat12Hour(format: CharSequence?) {
        mDescFormat12 = format

        chooseFormat()
        onTimeChanged()
    }

    @ViewDebug.ExportedProperty
    fun getFormat24Hour(): CharSequence? {
        return mFormat24
    }

    fun setFormat24Hour(format: CharSequence) {
        mFormat24 = format

        chooseFormat()
        onTimeChanged()
    }

    fun setContentDescriptionFormat24Hour(format: CharSequence) {
        mDescFormat24 = format

        chooseFormat()
        onTimeChanged()
    }

    fun refreshTime() {
        onTimeChanged()
        invalidate()
    }

    fun is24HourModeEnabled(): Boolean {
        return android.text.format.DateFormat.is24HourFormat(context)
    }

    fun getTimeZone(): String? {
        return mTimeZone
    }

    fun setTimeZone(timeZone: String?) {
        mTimeZone = timeZone

        createTime(timeZone)
        onTimeChanged()
    }

    fun getFormat(): CharSequence? {
        return mFormat
    }

    private fun chooseFormat() {
        val format24Requested = is24HourModeEnabled()
        val locale = context.resources.configuration.locale

        if (format24Requested) {
            mFormat = mFormat24 ?: mFormat12 ?: DateTimeConstants.CLOCK_FORMAT_12HR_AMPM_TZ
            mDescFormat = mDescFormat24 ?: mDescFormat12 ?: mFormat
        } else {
            mFormat = mFormat12 ?: mFormat24 ?: DateTimeUtils.getBestPatternForSkeleton(
                DateFormat.HOUR24_MINUTE + DateFormat.ABBR_SPECIFIC_TZ,
                locale
            )
            mDescFormat = mDescFormat12 ?: mDescFormat24 ?: mFormat
        }
        val hadSeconds = mHasSeconds
        mHasSeconds = mFormat?.contains('s') == true
        if (mShouldRunTicker && hadSeconds != mHasSeconds) {
            if (hadSeconds) handler.removeCallbacks(mTicker) else mTicker.run()
        }

        mFormatter = DateTimeFormatter.ofPattern(mFormat?.toString()!!, locale)
        mDescFormatter = DateTimeFormatter.ofPattern(mDescFormat?.toString()!!, locale)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mRegistered) {
            mRegistered = true
            registerReceiver()
            registerObserver()
            createTime(mTimeZone)
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!mShouldRunTicker && isVisible) {
            mShouldRunTicker = true
            if (mHasSeconds) {
                mTicker.run()
            } else {
                onTimeChanged()
            }
        } else if (mShouldRunTicker && !isVisible) {
            mShouldRunTicker = false
            handler.removeCallbacks(mTicker)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mRegistered) {
            unregisterReceiver()
            unregisterObserver()
            mRegistered = false
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)

        // OK, this is gross but needed. This class is supported by the
        // remote views mechanism and as a part of that the remote views
        // can be inflated by a context for another user without the app
        // having interact users permission - just for loading resources.
        // For example, when adding widgets from a managed profile to the
        // home screen. Therefore, we register the receiver as the user
        // the app is running as not the one the context is for.
        context.registerReceiver(mIntentReceiver, filter, null, handler)
    }

    private fun registerObserver() {
        if (mRegistered) {
            if (mFormatChangeObserver == null) {
                mFormatChangeObserver = FormatChangeObserver(handler)
            }
            val resolver = context.contentResolver
            val uri = Settings.System.getUriFor(Settings.System.TIME_12_24)
            resolver.registerContentObserver(
                uri, true,
                mFormatChangeObserver!!
            )
        }
    }

    private fun unregisterReceiver() {
        context.unregisterReceiver(mIntentReceiver)
    }

    private fun unregisterObserver() {
        if (mFormatChangeObserver != null) {
            val resolver = context.contentResolver
            resolver.unregisterContentObserver(mFormatChangeObserver!!)
        }
    }

    private fun onTimeChanged() {
        createTime(mTimeZone)
        subtitle = mTime.format(mFormatter)
        contentDescription = mTime.format(mDescFormatter)
    }
}