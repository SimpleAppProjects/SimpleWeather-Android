package com.thewizrd.simpleweather.controls

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.shape.ShapeAppearanceModel
import com.thewizrd.common.controls.BaseForecastItemViewModel
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.R
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter
import com.thewizrd.simpleweather.databinding.WeatherDetailPanelBinding
import java.util.Locale

class WeatherDetailItem : FrameLayout {
    companion object {
        /**
         * State indicating the group is expanded.
         */
        private val GROUP_EXPANDED_STATE_SET = intArrayOf(R.attr.state_expanded)
    }

    private lateinit var binding: WeatherDetailPanelBinding

    private var expandable = true
    private var expanded = false
    private var shouldShowBodyText = false

    private var onToggleListener: OnClickListener? = null
    fun setOnToggleListener(listener: OnClickListener?) {
        onToggleListener = listener
    }

    private lateinit var transitionSet: TransitionSet

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context)
    }

    var shapeAppearanceModel: ShapeAppearanceModel
        get() = binding.headerCard.shapeAppearanceModel
        set(value) {
            binding.headerCard.shapeAppearanceModel = value
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize(context: Context) {
        val inflater = LayoutInflater.from(context)

        binding = WeatherDetailPanelBinding.inflate(inflater, this, true)

        this.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.headerCard.setOnClickListener { toggle() }

        clipChildren = false
        clipToPadding = false

        binding.detailsContainer.adapter =
            DetailsItemGridAdapter(DetailsItemGridAdapter.ItemType.FORECAST)

        // Disable touch events on container
        // View does not scroll
        binding.detailsContainer.isFocusable = false
        binding.detailsContainer.isFocusableInTouchMode = false
        binding.detailsContainer.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP) {
                toggle()
            }
            true
        }

        transitionSet = TransitionSet().apply {
            duration = 250

            addTransition(
                ChangeBounds().apply {
                    interpolator = AccelerateDecelerateInterpolator()
                }
            )
            addTransition(
                Fade().apply {
                    interpolator = FastOutSlowInInterpolator()
                }
            )
        }
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
            val entering = !expanded

            TransitionManager.beginDelayedTransition(this, transitionSet)

            expanded = !expanded
            binding.bodyTextview.isVisible = entering && shouldShowBodyText
            binding.detailsContainer.isVisible = entering
            refreshDrawableState()

            onToggleListener?.onClick(this)
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
                binding.forecastHilo.text = "• / •"
                binding.forecastCondition.setText(R.string.placeholder_text)
                setExpandable(false)
                binding.bodyTextview.text = ""
            }
        }

        model?.extras?.values?.let {
            (binding.detailsContainer.adapter as? DetailsItemGridAdapter)?.updateItems(it)
        }

        binding.expandIcon.isVisible = isExpandable()

        binding.executePendingBindings()
    }

    private fun bindModel(forecastView: ForecastItemViewModel) {
        binding.forecastDate.text = forecastView.date
        binding.forecastIcon.weatherIcon = forecastView.weatherIcon
        binding.forecastHilo.text = String.format(
            Locale.ROOT, "%s / %s", forecastView.hiTemp, forecastView.loTemp
        )
        binding.forecastCondition.text = forecastView.condition

        binding.bodyTextview.text = forecastView.conditionLongDesc
        shouldShowBodyText = !forecastView.conditionLongDesc.isNullOrBlank()

        if (shouldShowBodyText || !forecastView.extras.isNullOrEmpty()) {
            setExpandable(true)
        } else {
            setExpandable(false)
        }
    }

    private fun bindModel(forecastView: HourlyForecastItemViewModel) {
        binding.forecastDate.text = forecastView.date
        binding.forecastIcon.weatherIcon = forecastView.weatherIcon
        binding.forecastHilo.text = forecastView.hiTemp
        binding.forecastCondition.text = forecastView.condition
        shouldShowBodyText = false

        binding.forecastCondition.post {
            val paint = binding.forecastCondition.paint
            val textWidth = paint.measureText(forecastView.condition ?: "")

            if (textWidth > binding.forecastCondition.width) {
                binding.bodyTextview.text = forecastView.condition
                shouldShowBodyText = true
            } else if (forecastView.extras.isEmpty()) {
                setExpandable(false)
            }

            binding.bodyTextview.isVisible = !binding.bodyTextview.text.isNullOrBlank()
            binding.expandIcon.isVisible = isExpandable() || shouldShowBodyText
        }

        setExpandable(!forecastView.extras.isNullOrEmpty())
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)

        if (isExpanded()) {
            mergeDrawableStates(drawableState, GROUP_EXPANDED_STATE_SET)
        }

        return drawableState
    }
}