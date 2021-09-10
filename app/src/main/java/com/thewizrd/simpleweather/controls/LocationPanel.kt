package com.thewizrd.simpleweather.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
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
import com.thewizrd.shared_resources.controls.TextViewDrawableCompat
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.LocationPanelBinding
import com.thewizrd.simpleweather.preferences.FeatureSettings
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LocationPanel : MaterialCardView, CoroutineScope {
    private lateinit var binding: LocationPanelBinding
    private var overlayDrawable: Drawable? = null
    private lateinit var mGlide: RequestManager

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    constructor(context: Context) : super(context) {
        initialize(getContext())
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(getContext())
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(getContext())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = Job()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job.cancel()
    }

    private fun initialize(context: Context) {
        val inflater = LayoutInflater.from(context)
        binding = LocationPanelBinding.inflate(inflater, this, true)

        val height = context.resources.getDimensionPixelSize(R.dimen.location_panel_height)
        this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)

        setCardBackgroundColor(ContextUtils.getColor(context, R.attr.colorSurface))
        overlayDrawable = ContextCompat.getDrawable(context, R.drawable.background_overlay)

        cardElevation = ContextUtils.dpToPx(context, 2f)
        strokeWidth = ContextUtils.dpToPx(context, 1f).toInt()
        setStrokeColor(
            AppCompatResources.getColorStateList(
                context, if (FeatureSettings.isLocationPanelImageEnabled()) {
                    R.color.location_panel_card_stroke_imageon
                } else {
                    R.color.location_panel_card_stroke_imageoff
                }
            )
        )
        checkedIconTint = if (FeatureSettings.isLocationPanelImageEnabled()) {
            ColorStateList.valueOf(Colors.WHITE)
        } else ColorStateList.valueOf(
            ContextUtils.getColor(context, R.attr.colorPrimary)
        )
        setRippleColorResource(
            if (FeatureSettings.isLocationPanelImageEnabled()) {
                R.color.location_panel_ripple_imageon
            } else {
                R.color.location_panel_ripple_imageoff
            }
        )
        setCardForegroundColor(
            AppCompatResources.getColorStateList(
                context,
                if (FeatureSettings.isLocationPanelImageEnabled()) {
                    R.color.location_panel_foreground_imageon
                } else {
                    R.color.location_panel_foreground_imageoff
                }
            )
        )

        mGlide = Glide.with(this)
        showLoading(true)
    }

    fun setWeatherBackground(panelView: LocationPanelViewModel) {
        setWeatherBackground(panelView, false)
    }

    fun setWeatherBackground(panelView: LocationPanelViewModel, skipCache: Boolean) {
        launch(Dispatchers.IO) {
            if (panelView.imageData == null) {
                panelView.updateBackground()
            }

            withContext(Dispatchers.Main) {
                val backgroundUri = panelView.imageData?.imageURI

                // Background
                if (FeatureSettings.isLocationPanelImageEnabled() && backgroundUri != null) {
                    mGlide.asBitmap()
                            .load(backgroundUri)
                            .apply(
                                RequestOptions
                                    .centerCropTransform()
                                    .format(DecodeFormat.PREFER_RGB_565)
                                    .error(null)
                                    .placeholder(null)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .skipMemoryCache(skipCache))
                            .transition(BitmapTransitionOptions.withCrossFade())
                            .into(object : BitmapImageViewTarget(binding.imageView) {
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
                            })
                } else {
                    clearBackground()
                    showLoading(false)
                }
            }
        }
    }

    @MainThread
    fun clearBackground() {
        launch(Dispatchers.Main.immediate) {
            mGlide.clear(binding.imageView)
            binding.imageView.setImageDrawable(null)
        }
    }

    @MainThread
    fun bindModel(model: LocationPanelViewModel) {
        binding.viewModel = model
        binding.executePendingBindings()

        // Do this before checkable state is changed to disable check state
        this.isChecked = model.isChecked
        this.isCheckable = model.isEditMode
        // Do this after if checkable state is now enabled
        this.isChecked = model.isChecked

        if (model.locationType == LocationType.GPS.value) {
            binding.gpsIcon.visibility = View.VISIBLE
        } else {
            binding.gpsIcon.visibility = View.GONE
        }

        this.tag = model.locationData

        if (FeatureSettings.isLocationPanelImageEnabled() && model.imageData != null ||
            !FeatureSettings.isLocationPanelImageEnabled() && model.locationName != null && tag != null
        ) {
            showLoading(false)
        }
    }

    fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) VISIBLE else GONE
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
        if (FeatureSettings.isLocationPanelImageEnabled()) {
            if (isDragged) {
                strokeDp = 3f
            } else if (isChecked) {
                strokeDp = 2f
            }
        } else {
            strokeDp = if (isDragged) 2f else 1f
        }
        strokeWidth = ContextUtils.dpToPx(context, strokeDp).toInt()
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
            view.setTextColor(ContextUtils.getColor(view.context, R.attr.colorOnSurface))
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
                    ContextUtils.getColor(view.context, R.attr.colorOnSurface)
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
                    ContextUtils.getColor(view.context, R.attr.colorOnSurface)
                }
            )
        )
    }
}