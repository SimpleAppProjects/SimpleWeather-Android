package com.thewizrd.simpleweather.preferences.chippreference

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ArrayRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.R as materialRes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.simpleweather.R


/**
 * A [Preference] that displays a list of entries as a [ChipGroup]. Based on [ListPreference]
 *
 *
 * This preference saves a string value. This string will be the value from the
 * [entryValues] array.
 *
 * @property entries android:entries
 * @property entryValues android:entryValues
 */
class ChipPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.chipPreferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {
    private lateinit var mEntries: Array<CharSequence>
    private lateinit var mEntryValues: Array<CharSequence>
    private var mValue: String? = null
    private var mValueSet = false

    private lateinit var chipGroup: ChipGroup

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.ChipPreference, defStyleAttr, defStyleRes
        ).use {
            mEntries = it.getTextArray(R.styleable.ChipPreference_android_entries)
            mEntryValues = it.getTextArray(R.styleable.ChipPreference_android_entryValues)
        }
    }

    constructor(
        context: Context,
        entries: Array<CharSequence>,
        entryValues: Array<CharSequence>
    ) : this(context) {
        mEntries = entries
        mEntryValues = entryValues
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        chipGroup = holder.findViewById(R.id.chip_group) as ChipGroup

        chipGroup.removeAllViews()

        val entries = mEntries
        val entryValues = mEntryValues

        entries.forEachIndexed { index, entry ->
            val chip = Chip(holder.itemView.context).apply {
                id = View.generateViewId()
                minimumWidth = context.dpToPx(60f).toInt()
                text = entry
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                isCheckable = true
                isCheckedIconVisible = false
                chipStrokeWidth = 1f
                chipStrokeColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        Colors.TRANSPARENT,
                        ContextCompat.getColor(
                            context,
                            materialRes.color.material_on_surface_stroke
                        )
                    )
                )
                shapeAppearanceModel = if (index == 0 && entries.isNotEmpty()) {
                    ShapeAppearanceModel.builder()
                        .setTopLeftCornerSize(RelativeCornerSize(0.1f))
                        .setBottomLeftCornerSize(RelativeCornerSize(0.1f))
                        .build()
                } else if (index == entries.size - 1) {
                    ShapeAppearanceModel.builder()
                        .setTopRightCornerSize(RelativeCornerSize(0.1f))
                        .setBottomRightCornerSize(RelativeCornerSize(0.1f))
                        .build()
                } else {
                    ShapeAppearanceModel.builder().build()
                }
                chipBackgroundColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        context.getAttrColor(androidx.appcompat.R.attr.colorPrimaryDark),
                        context.getAttrColor(materialRes.attr.colorSurfaceContainer)
                    )
                )
                setTextColor(
                    ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            context.getAttrColor(materialRes.attr.colorOnPrimary),
                            context.getAttrColor(materialRes.attr.colorOnSurface)
                        )
                    )
                )

                setOnClickListener {
                    value = entryValues[index].toString()
                    chipGroup.check(id)
                }
            }

            chipGroup.addView(chip)

            if (TextUtils.equals(mValue, entryValues[index])) {
                chipGroup.check(chip.id)
            }
        }
    }

    var entries: Array<CharSequence>
        get() = mEntries
        set(value) {
            mEntries = value
        }

    fun setEntries(@ArrayRes entriesResId: Int) {
        entries = context.resources.getTextArray(entriesResId)
    }

    var entryValues: Array<CharSequence>
        get() = mEntryValues
        set(value) {
            mEntryValues = value
        }

    fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        entryValues = context.resources.getTextArray(entryValuesResId)
    }

    var value: String?
        get() = mValue
        set(value) {
            // Always persist/notify the first time.
            val changed = !TextUtils.equals(mValue, value)
            if (changed || !mValueSet) {
                mValue = value
                mValueSet = true
                persistString(value)
                if (changed) {
                    notifyChanged()
                }
            }
        }

    fun getEntry(): CharSequence? {
        val index: Int = getValueIndex()
        return if (index >= 0) mEntries[index] else null
    }

    fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            for (i in mEntryValues.indices.reversed()) {
                if (TextUtils.equals(mEntryValues[i].toString(), value)) {
                    return i
                }
            }
        }

        return -1
    }

    fun setValueIndex(index: Int) {
        value = mEntryValues[index].toString()
    }

    private fun getValueIndex(): Int {
        return findIndexOfValue(mValue)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString(defaultValue as String?)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        val myState = SavedState(superState)
        myState.mValue = value
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        value = myState.mValue
    }


    private class SavedState : BaseSavedState {
        var mValue: String? = null

        internal constructor(source: Parcel) : super(source) {
            mValue = source.readString()
        }

        internal constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(mValue)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel): SavedState {
                return SavedState(`in`)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

}