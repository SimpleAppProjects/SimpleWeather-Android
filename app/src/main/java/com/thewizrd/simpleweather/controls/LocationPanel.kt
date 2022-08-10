package com.thewizrd.simpleweather.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.card.MaterialCardView
import com.thewizrd.common.controls.TextViewDrawableCompat
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LocationPanelBinding
import com.thewizrd.simpleweather.preferences.FeatureSettings
import kotlinx.coroutines.*

class LocationPanel : MaterialCardView {
    private lateinit var binding: LocationPanelBinding
    private var overlayDrawable: Drawable? = null
    private lateinit var mGlide: RequestManager

    private var scope = CoroutineScope(Dispatchers.Main.immediate + Job())

    private lateinit var bitmapImageViewTarget: BitmapImageViewTarget

    constructor(context: Context) : super(context) {
        initialize(getContext())
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(getContext())
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(getContext())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!scope.isActive) {
            refreshScope()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    private fun refreshScope() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    }

    private fun initialize(context: Context) {
        val inflater = LayoutInflater.from(context)
        binding = LocationPanelBinding.inflate(inflater, this, true)

        val height = context.resources.getDimensionPixelSize(R.dimen.location_panel_height)
        this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)

        setCardBackgroundColor(context.getAttrColor(R.attr.colorSurface))
        overlayDrawable = ContextCompat.getDrawable(context, R.drawable.background_overlay)

        cardElevation = context.dpToPx(2f)
        strokeWidth = context.dpToPx(1f).toInt()
        setStrokeColor(
            AppCompatResources.getColorStateList(
                context, if (FeatureSettings.isLocationPanelImageEnabled) {
                    R.color.location_panel_card_stroke_imageon
                } else {
                    R.color.location_panel_card_stroke_imageoff
                }
            )
        )
        checkedIconTint = if (FeatureSettings.isLocationPanelImageEnabled) {
            ColorStateList.valueOf(Colors.WHITE)
        } else ColorStateList.valueOf(
            context.getAttrColor(R.attr.colorPrimary)
        )
        setRippleColorResource(
            if (FeatureSettings.isLocationPanelImageEnabled) {
                R.color.location_panel_ripple_imageon
            } else {
                R.color.location_panel_ripple_imageoff
            }
        )
        setCardForegroundColor(
            AppCompatResources.getColorStateList(
                context,
                if (FeatureSettings.isLocationPanelImageEnabled) {
                    R.color.location_panel_foreground_imageon
                } else {
                    R.color.location_panel_foreground_imageoff
                }
            )
        )

        mGlide = Glide.with(this)
        showLoading(true)

        bitmapImageViewTarget = object : BitmapImageViewTarget(binding.imageView) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                super.onResourceReady(resource, transition)
                binding.imageOverlay.background = overlayDrawable
                showLoading(false)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                binding.imageOverlay.background = null
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                super.onLoadCleared(placeholder)
                binding.imageOverlay.background = null
            }

            override fun onLoadStarted(placeholder: Drawable?) {
                super.onLoadStarted(placeholder)
                showLoading(true)
                binding.imageOverlay.background = null
            }
        }
    }

    fun setWeatherBackground(panelView: LocationPanelUiModel, skipCache: Boolean = false) {
        scope.launch {
            if (panelView.imageData == null) {
                panelView.updateBackground()
            }

            val backgroundUri = panelView.imageData?.imageURI

            // Background
            if (FeatureSettings.isLocationPanelImageEnabled && backgroundUri != null) {
                mGlide.asBitmap()
                    .load(backgroundUri)
                    .apply(
                        RequestOptions
                            .centerCropTransform()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .error(null)
                            .placeholder(ColorDrawable(Colors.LIGHTGRAY))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(skipCache)
                    )
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .into(bitmapImageViewTarget)
            } else {
                clearBackground()
                showLoading(false)
            }
        }
    }

    @MainThread
    fun clearBackground() {
        scope.launch(Dispatchers.Main.immediate) {
            mGlide.clear(binding.imageView)
            binding.imageView.setImageDrawable(null)
        }
    }

    @MainThread
    fun bindModel(model: LocationPanelUiModel) {
        binding.viewModel = model
        binding.executePendingBindings()

        // Do this before checkable state is changed to disable check state
        this.isChecked = model.isChecked
        this.isCheckable = model.isEditMode
        // Do this after if checkable state is now enabled
        this.isChecked = model.isChecked

        binding.gpsIcon.isVisible = model.locationType == LocationType.GPS.value

        this.tag = model.locationData

        if (FeatureSettings.isLocationPanelImageEnabled && model.imageData != null ||
            !FeatureSettings.isLocationPanelImageEnabled && model.locationName != null && tag != null
        ) {
            showLoading(false)
        }
    }

    fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        isClickable = !show
    }

    override fun setDragged(dragged: Boolean) {
        super.setDragged(dragged)
        setStroke()
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        setStroke()
    }

    private fun setStroke() {
        var strokeDp = 1f
        if (FeatureSettings.isLocationPanelImageEnabled) {
            if (isDragged) {
                strokeDp = 3f
            } else if (isChecked) {
                strokeDp = 2f
            }
        } else {
            strokeDp = if (isDragged) 2f else 1f
        }
        strokeWidth = context.dpToPx(strokeDp).toInt()
    }
}

object LocationPanelBindingAdapter {
    @JvmStatic
    @BindingAdapter("locImageEnabled")
    fun updateWithPanelImage(view: TextView, panelImageEnabled: Boolean) {
        if (panelImageEnabled) {
            view.setShadowLayer(1f, view.shadowDx, view.shadowDy, Colors.BLACK)
            view.setTextColor(Colors.WHITE)
        } else {
            view.setShadowLayer(0f, view.shadowDx, view.shadowDy, Colors.TRANSPARENT)
            view.setTextColor(view.context.getAttrColor(R.attr.colorOnSurface))
        }
    }

    @JvmStatic
    @BindingAdapter("locImageEnabled")
    fun updateWithPanelImage(view: TextViewDrawableCompat, panelImageEnabled: Boolean) {
        updateWithPanelImage(view as TextView, panelImageEnabled)

        TextViewCompat.setCompoundDrawableTintList(
            view,
            ColorStateList.valueOf(
                if (panelImageEnabled) {
                    Colors.WHITE
                } else {
                    view.context.getAttrColor(R.attr.colorOnSurface)
                }
            )
        )
    }

    @JvmStatic
    @BindingAdapter("locImageEnabled")
    fun updateWithPanelImage(view: ImageView, panelImageEnabled: Boolean) {
        ImageViewCompat.setImageTintList(
            view,
            ColorStateList.valueOf(
                if (panelImageEnabled) {
                    Colors.WHITE
                } else {
                    view.context.getAttrColor(R.attr.colorOnSurface)
                }
            )
        )
    }
}