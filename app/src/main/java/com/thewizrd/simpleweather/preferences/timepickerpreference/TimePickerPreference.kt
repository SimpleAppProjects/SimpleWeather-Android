package com.thewizrd.simpleweather.preferences.timepickerpreference

import android.content.Context
import android.content.res.TypedArray
import android.text.format.DateFormat
import android.util.AttributeSet
import androidx.annotation.IntRange
import androidx.preference.DialogPreference
import java.util.*

/**
 * Based on AOSP TimePickerPreference implementation
 */
class TimePickerPreference : DialogPreference {
    var hourOfDay = 0
        private set
    var minute = 0
        private set
    private var mValueSet: Boolean = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setTime(@IntRange(from = 0, to = 23) hourOfDay: Int, @IntRange(from = 0, to = 59) minute: Int) {
        this.hourOfDay = hourOfDay
        this.minute = minute
        updateSummary()

        persistString(getValueInternal())
        notifyChanged()
    }

    private fun updateSummary() {
        val c = Calendar.getInstance()
        c[Calendar.HOUR_OF_DAY] = hourOfDay
        c[Calendar.MINUTE] = minute
        val time = DateFormat.getTimeFormat(context).format(c.time)
        summary = time
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        val defaultValue = a.getString(index)

        if (!defaultValue.isNullOrBlank()) {
            val splits = defaultValue.split(":")
            if (splits.size == 2) {
                val h = splits[0].toIntOrNull()
                val m = splits[1].toIntOrNull()

                if (h != null && h >= 0 && m != null && m >= 0) {
                    return defaultValue
                }
            }
        }

        return "08:00"
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val defaultTime = defaultValue?.toString() ?: "08:00"
        setTime(getPersistedString(defaultTime))
    }

    fun setTime(time: String) {
        val splits = time.split(":")
        if (splits.size == 2) {
            val h = splits[0].toIntOrNull()
            val m = splits[1].toIntOrNull()

            if (h != null && h >= 0 && m != null && m >= 0) {
                setTime(h, m)
            } else {
                throw IllegalArgumentException("Invalid time format! Expected HH:mm")
            }
        }
    }

    private fun getValueInternal(): String {
        return String.format(Locale.ROOT, "%02d:%02d", hourOfDay, minute)
    }
}