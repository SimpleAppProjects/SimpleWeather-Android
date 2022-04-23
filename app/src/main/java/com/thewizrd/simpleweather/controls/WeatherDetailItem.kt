package com.thewizrd.simpleweather.controls

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TabStopSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.thewizrd.common.controls.BaseForecastItemViewModel
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.StringUtils.lineSeparator
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.WeatherDetailPanelBinding
import java.util.*

class WeatherDetailItem : LinearLayout {
    companion object {
        /**
         * State indicating the group is expanded.
         */
        private val GROUP_EXPANDED_STATE_SET = intArrayOf(R.attr.state_expanded)
    }

    private lateinit var binding: WeatherDetailPanelBinding

    private var expandable = true
    private var expanded = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        val inflater = LayoutInflater.from(context)

        binding = WeatherDetailPanelBinding.inflate(inflater, this, true)

        this.orientation = VERTICAL
        this.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.headerCard.setOnClickListener { toggle() }
    }

    fun isExpandable(): Boolean {
        return expandable
    }

    fun setExpandable(expandable: Boolean) {
        this.expandable = expandable
    }

    fun isExpanded(): Boolean {
        return expanded
    }

    fun setExpanded(expanded: Boolean) {
        if (this.expanded != expanded) {
            toggle()
        }
    }

    fun toggle() {
        if (isExpandable() && isEnabled) {
            expanded = !expanded
            binding.bodyCard.visibility = if (expanded) VISIBLE else GONE
            refreshDrawableState()
        }
    }

    fun bind(model: BaseForecastItemViewModel?) {
        // Reset expanded state
        setExpandable(true)
        setExpanded(false)

        when (model) {
            is ForecastItemViewModel -> {
                bindModel(model)
            }
            is HourlyForecastItemViewModel -> {
                bindModel(model)
            }
            else -> {
                binding.forecastDate.setText(R.string.placeholder_text)
                binding.forecastIcon.setImageResource(R.drawable.wi_na)
                binding.forecastCondition.setText(R.string.placeholder_text)
                clearForecastExtras()
                setExpandable(false)
                binding.bodyTextview.text = ""
            }
        }

        binding.executePendingBindings()

        val wim = sharedDeps.weatherIconsManager
        if (binding.forecastExtraPop.iconProvider != null) {
            binding.forecastExtraPop.showAsMonochrome =
                wim.shouldUseMonochrome(binding.forecastExtraPop.iconProvider)
        } else {
            binding.forecastExtraPop.showAsMonochrome = wim.shouldUseMonochrome()
        }
        if (binding.forecastExtraClouds.iconProvider != null) {
            binding.forecastExtraClouds.showAsMonochrome =
                wim.shouldUseMonochrome(binding.forecastExtraClouds.iconProvider)
        } else {
            binding.forecastExtraClouds.showAsMonochrome = wim.shouldUseMonochrome()
        }
        if (binding.forecastExtraWindspeed.iconProvider != null) {
            binding.forecastExtraWindspeed.showAsMonochrome =
                wim.shouldUseMonochrome(binding.forecastExtraWindspeed.iconProvider)
        } else {
            binding.forecastExtraWindspeed.showAsMonochrome = wim.shouldUseMonochrome()
        }
    }

    private fun clearForecastExtras() {
        binding.forecastExtraPop.visibility = GONE
        binding.forecastExtraPop.text = ""
        binding.forecastExtraClouds.visibility = GONE
        binding.forecastExtraClouds.text = ""
        binding.forecastExtraWindspeed.visibility = GONE
        binding.forecastExtraWindspeed.text = ""
    }

    private fun bindModel(forecastView: ForecastItemViewModel) {
        binding.forecastDate.text = forecastView.date
        binding.forecastIcon.weatherIcon = forecastView.weatherIcon
        binding.forecastCondition.text = String.format(
            Locale.ROOT, "%s / %s - %s",
            forecastView.hiTemp, forecastView.loTemp, forecastView.condition
        )
        clearForecastExtras()

        val lineSeparator = lineSeparator()

        val sb = SpannableStringBuilder()

        if (!forecastView.conditionLongDesc.isNullOrBlank()) {
            sb.append(forecastView.conditionLongDesc)
                .append(lineSeparator)
                .append(lineSeparator)
        }

        if (!forecastView.extras.isNullOrEmpty()) {
            if (forecastView.conditionLongDesc.isNullOrBlank()) {
                val paint = binding.forecastCondition.paint
                val layout = binding.forecastCondition.layout
                val textWidth = paint.measureText(forecastView.condition)

                if (layout != null && textWidth > layout.width) {
                    sb.append(forecastView.condition)
                        .append(lineSeparator())
                        .append(lineSeparator())
                }
            }

            forecastView.extras.values.forEachIndexed { i, detailItem ->
                if (detailItem.detailsType == WeatherDetailsType.POPCHANCE) {
                    binding.forecastExtraPop.text = detailItem.value
                    binding.forecastExtraPop.visibility = VISIBLE
                    return@forEachIndexed
                } else if (detailItem.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                    binding.forecastExtraClouds.text = detailItem.value
                    binding.forecastExtraClouds.visibility = VISIBLE
                    return@forEachIndexed
                } else if (detailItem.detailsType == WeatherDetailsType.WINDSPEED) {
                    binding.forecastExtraWindspeed.text = detailItem.value
                    binding.forecastExtraWindspeed.visibility = VISIBLE
                    return@forEachIndexed
                }

                var start = sb.length
                sb.append(detailItem.label)
                sb.append("\t")
                val colorSecondary = context.getAttrColor(android.R.attr.textColorSecondary)
                sb.setSpan(
                    ForegroundColorSpan(colorSecondary),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    TabStopSpan.Standard(context.dpToPx(150f).toInt()),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                start = sb.length
                sb.append(detailItem.value)
                if (i < forecastView.extras.size - 1) sb.append(lineSeparator)
                val colorPrimary = context.getAttrColor(android.R.attr.textColorPrimary)
                sb.setSpan(
                    ForegroundColorSpan(colorPrimary),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (sb.length >= lineSeparator.length) {
                val start = sb.length - lineSeparator.length
                val lastSeq = sb.subSequence(start, sb.length).toString()
                if (lastSeq == lineSeparator) {
                    sb.replace(start, sb.length, "")
                }
            }
        }

        if (sb.isNotEmpty()) {
            binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE)
            setExpandable(true)
        } else {
            setExpandable(false)
        }
    }

    private fun bindModel(forecastView: HourlyForecastItemViewModel) {
        binding.forecastDate.text = forecastView.date
        binding.forecastIcon.weatherIcon = forecastView.weatherIcon
        binding.forecastCondition.text = String.format(
            Locale.ROOT, "%s - %s",
            forecastView.hiTemp, forecastView.condition
        )
        clearForecastExtras()

        val lineSeparator = lineSeparator()

        if (!forecastView.extras.isNullOrEmpty()) {
            setExpandable(true)

            val sb = SpannableStringBuilder()

            forecastView.extras.values.forEachIndexed { i, detailItem ->
                if (detailItem.detailsType == WeatherDetailsType.POPCHANCE) {
                    binding.forecastExtraPop.text = detailItem.value
                    binding.forecastExtraPop.visibility = VISIBLE
                    return@forEachIndexed
                } else if (detailItem.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                    binding.forecastExtraClouds.text = detailItem.value
                    binding.forecastExtraClouds.visibility = VISIBLE
                    return@forEachIndexed
                } else if (detailItem.detailsType == WeatherDetailsType.WINDSPEED) {
                    binding.forecastExtraWindspeed.text = detailItem.value
                    binding.forecastExtraWindspeed.visibility = VISIBLE
                    return@forEachIndexed
                }

                var start = sb.length
                sb.append(detailItem.label)
                sb.append("\t")
                val colorSecondary = context.getAttrColor(android.R.attr.textColorSecondary)
                sb.setSpan(
                    ForegroundColorSpan(colorSecondary),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    TabStopSpan.Standard(context.dpToPx(150f).toInt()),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                start = sb.length
                sb.append(detailItem.value)
                if (i < forecastView.extras.size - 1) sb.append(lineSeparator)
                val colorPrimary = context.getAttrColor(android.R.attr.textColorPrimary)
                sb.setSpan(
                    ForegroundColorSpan(colorPrimary),
                    start,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (sb.length >= lineSeparator.length) {
                val start = sb.length - lineSeparator.length
                val lastSeq = sb.subSequence(start, sb.length).toString()
                if (lastSeq == lineSeparator) {
                    sb.replace(start, sb.length, "")
                }
            }

            binding.forecastCondition.post {
                val paint = binding.forecastCondition.paint
                val textWidth = paint.measureText(forecastView.condition)

                if (textWidth > binding.forecastCondition.width) {
                    sb.insert(0, forecastView.condition)
                        .append(lineSeparator())
                        .append(lineSeparator())
                } else if (sb.isEmpty()) {
                    setExpandable(false)
                    return@post
                }

                binding.bodyTextview.setText(sb, TextView.BufferType.SPANNABLE)
            }
        } else {
            setExpandable(false)
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)

        if (isExpanded()) {
            mergeDrawableStates(drawableState, GROUP_EXPANDED_STATE_SET)
        }

        return drawableState
    }
}