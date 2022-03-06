package com.thewizrd.shared_resources.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import com.thewizrd.shared_resources.R
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class SliderPreference : Preference {
    companion object {
        private const val TAG = "SliderPreference"
    }

    private var mSliderValue: Float = 0f
    private var mMin: Float = 0f
    private var mMax: Float = 1f
    private var mStepSize: Float = 0f
    private var mTrackingTouch: Boolean = false
    private var mSlider: Slider? = null
    private var mSliderValueTextView: TextView? = null
    private var mAdjustable: Boolean = false
    private var mShowSliderValue: Boolean = false
    private var mUpdatesContinuously: Boolean = false
    private var mShowSummary: Boolean = false

    private val mSliderChangeListener = Slider.OnChangeListener { slider, value, fromUser ->
        if (fromUser && (mUpdatesContinuously || !mTrackingTouch)) {
            syncValueInternal(slider)
        } else {
            // We always want to update the text while the slider is being dragged
            updateLabelValue(value + mMin)
        }
    }

    @SuppressLint("RestrictedApi")
    private val mSlideTouchListener = object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            mTrackingTouch = true
        }

        override fun onStopTrackingTouch(slider: Slider) {
            mTrackingTouch = false
            if (slider.value != mSliderValue) {
                syncValueInternal(slider)
            }
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.sliderPreferenceStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.SliderPreference,
            defStyleAttr,
            defStyleRes
        )

        mMin = a.getFloat(R.styleable.SliderPreference_sliderMin, 0f)
        setMax(a.getFloat(R.styleable.SliderPreference_sliderMax, 100f))
        setSliderStepSize(a.getFloat(R.styleable.SliderPreference_sliderStepSize, 0f))
        mAdjustable = a.getBoolean(R.styleable.SliderPreference_adjustable, true)
        mShowSliderValue = a.getBoolean(R.styleable.SliderPreference_showSliderValue, false)
        mUpdatesContinuously = a.getBoolean(R.styleable.SliderPreference_updatesContinuously, false)
        mShowSummary = a.getBoolean(R.styleable.SliderPreference_showSummary, false)

        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mSlider = holder.findViewById(R.id.seekbar) as Slider?
        mSliderValueTextView = holder.findViewById(R.id.seekbar_value) as TextView?
        val summaryView = holder.findViewById(android.R.id.summary) as TextView?

        if (mShowSliderValue) {
            mSliderValueTextView?.isVisible = true
        } else {
            mSliderValueTextView?.isVisible = false
            mSliderValueTextView = null
        }

        summaryView?.isVisible = mShowSummary

        if (mSlider == null) {
            Log.e(TAG, "Slider view is null in onBindViewHolder.")
            return
        }
        mSlider?.addOnChangeListener(mSliderChangeListener)
        mSlider?.addOnSliderTouchListener(mSlideTouchListener)
        mSlider?.valueFrom = mMin
        mSlider?.valueTo = mMax
        mSlider?.stepSize = mStepSize

        mSlider?.value = normalizeValue(mSliderValue)
        updateLabelValue(mSliderValue)
        mSlider?.isEnabled = isEnabled
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        var initialValue = defaultValue

        if (initialValue == null) {
            initialValue = 0f
        }

        setValue(getPersistedFloat(initialValue as Float))
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getFloat(index, 0f)
    }

    /**
     * Gets the lower bound set on the {@link Slider}.
     *
     * @return The lower bound set
     */
    fun getMin(): Float {
        return mMin
    }

    /**
     * Sets the lower bound on the {@link Slider}.
     *
     * @param min The lower bound to set
     */
    fun setMin(min: Float) {
        var minSet = min

        if (minSet > mMax) {
            minSet = mMax
        }

        if (minSet != mMin) {
            mMin = minSet
            notifyChanged()
        }
    }

    fun getSliderStepSize(): Float {
        return mStepSize
    }

    fun setSliderStepSize(stepSize: Float) {
        if (mStepSize != stepSize) {
            mStepSize = min(mMax - mMin, stepSize)
            notifyChanged()
        }
    }

    /**
     * Gets the upper bound set on the {@link Slider}.
     *
     * @return The upper bound set
     */
    fun getMax(): Float {
        return mMax
    }

    /**
     * Sets the upper bound on the {@link Slider}.
     *
     * @param max The upper bound to set
     */
    fun setMax(max: Float) {
        var maxSet = max

        if (maxSet < mMin) {
            maxSet = mMin
        }

        if (maxSet != mMax) {
            mMax = maxSet
            notifyChanged()
        }
    }


    /**
     * Gets whether the [Slider] should respond to the left/right keys.
     *
     * @return Whether the [Slider] should respond to the left/right keys
     */
    fun isAdjustable(): Boolean {
        return mAdjustable
    }

    /**
     * Sets whether the [Slider] should respond to the left/right keys.
     *
     * @param adjustable Whether the [Slider] should respond to the left/right keys
     */
    fun setAdjustable(adjustable: Boolean) {
        mAdjustable = adjustable
    }

    /**
     * Gets whether the [SliderPreference] should continuously save the [Slider] value
     * while it is being dragged. Note that when the value is true,
     * [Preference.OnPreferenceChangeListener] will be called continuously as well.
     *
     * @return Whether the [SliderPreference] should continuously save the [Slider]
     * value while it is being dragged
     * @see .setUpdatesContinuously
     */
    fun getUpdatesContinuously(): Boolean {
        return mUpdatesContinuously
    }

    /**
     * Sets whether the [SliderPreference] should continuously save the [Slider] value
     * while it is being dragged.
     *
     * @param updatesContinuously Whether the [SliderPreference] should continuously save
     * the [Slider] value while it is being dragged
     * @see .getUpdatesContinuously
     */
    fun setUpdatesContinuously(updatesContinuously: Boolean) {
        mUpdatesContinuously = updatesContinuously
    }

    /**
     * Gets whether the current [Slider] value is displayed to the user.
     *
     * @return Whether the current [Slider] value is displayed to the user
     * @see .setShowSliderValue
     */
    fun getShowSliderValue(): Boolean {
        return mShowSliderValue
    }

    /**
     * Sets whether the current [Slider] value is displayed to the user.
     *
     * @param showSliderValue Whether the current [Slider] value is displayed to the user
     * @see .getShowSliderValue
     */
    fun setShowSliderValue(showSliderValue: Boolean) {
        mShowSliderValue = showSliderValue
        notifyChanged()
    }

    private fun setValueInternal(sliderValue: Float, notifyChanged: Boolean) {
        var sliderValueSet = sliderValue
        if (sliderValueSet < mMin) {
            sliderValueSet = mMin
        }
        if (sliderValueSet > mMax) {
            sliderValueSet = mMax
        }
        if (sliderValueSet != mSliderValue) {
            mSliderValue = sliderValueSet
            updateLabelValue(mSliderValue)
            persistFloat(sliderValueSet)
            if (notifyChanged) {
                notifyChanged()
            }
        }
    }

    /**
     * Gets the current progress of the [Slider].
     *
     * @return The current progress of the [Slider]
     */
    fun getValue(): Float {
        return mSliderValue
    }

    /**
     * Sets the current progress of the [Slider].
     *
     * @param sliderValue The current progress of the [Slider]
     */
    fun setValue(sliderValue: Float) {
        setValueInternal(sliderValue, true)
    }

    /**
     * Persist the [Slider]'s Slider value if callChangeListener returns true, otherwise
     * set the [Slider]'s value to the stored value.
     */
    private fun syncValueInternal(slider: Slider) {
        val sliderValue = slider.value
        if (sliderValue != mSliderValue) {
            if (callChangeListener(sliderValue)) {
                setValueInternal(sliderValue, false)
            } else {
                slider.value = mSliderValue
                updateLabelValue(mSliderValue)
            }
        }
    }

    /**
     * Attempts to update the TextView label that displays the current value.
     *
     * @param value the value to display next to the [Slider]
     */
    private fun updateLabelValue(value: Float) {
        mSliderValueTextView?.text = value.toString()
    }

    fun getShowSummary(): Boolean {
        return mShowSummary
    }

    fun setShowSummary(show: Boolean) {
        mShowSummary = show
    }

    /* Utils */
    private fun valueLandsOnTick(value: Float): Boolean {
        // Check that the value is a multiple of stepSize given the offset of valueFrom.
        return isMultipleOfStepSize(value - mMin)
    }

    private fun isMultipleOfStepSize(value: Float): Boolean {
        // We're using BigDecimal here to avoid floating point rounding errors.
        val result = BigDecimal(value.toString())
            .divide(BigDecimal(mStepSize.toString()), MathContext.DECIMAL64)
            .toDouble()

        // If the result is a whole number, it means the value is a multiple of stepSize.
        return abs(result.roundToInt() - result) < .0001
    }

    private fun normalizeValue(value: Float): Float {
        return if (valueLandsOnTick(value)) {
            value
        } else {
            var normalizedValue = mMin

            while (normalizedValue < value) {
                normalizedValue += mStepSize
            }

            if (normalizedValue > mMax) {
                normalizedValue = mMax - mStepSize
            }

            normalizedValue
        }
    }
}