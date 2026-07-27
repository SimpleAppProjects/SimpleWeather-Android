package com.thewizrd.simpleweather.preferences

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.material.R as materialRes
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.common.utils.isAdapterOfTypeOrConcatContains
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSettingsBinding
import com.thewizrd.simpleweather.snackbar.SnackbarManager

abstract class ToolbarPreferenceFragmentCompat : WindowColorPreferenceFragmentCompat() {
    // Views
    private lateinit var binding: FragmentSettingsBinding

    val appBarLayout: AppBarLayout
        get() = binding.appBar
    val rootView: CoordinatorLayout
        get() = binding.coordinatorLayout
    val collapsingToolbar: CollapsingToolbarLayout
        get() = binding.collapsingToolbar
    val toolbar: MaterialToolbar
        get() = binding.toolbar

    override fun createSnackManager(activity: Activity): SnackbarManager? {
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }

    @get:StringRes
    protected abstract val titleResId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val lp = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.behavior = ScrollingViewBehavior()
        binding.root.addView(inflatedView, lp)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceScreen.tintIcons(requireContext().getAttrColor(androidx.appcompat.R.attr.colorPrimary))

        // Toolbar
        binding.toolbar.setTitle(titleResId)
        binding.appBar.liftOnScrollTargetViewId = listView.id
    }

    override fun updateWindowColors() {
        super.updateWindowColors()
        updateWindowColors(settingsManager.getUserThemeMode())
    }

    @SuppressLint("NotifyDataSetChanged")
    protected fun updateWindowColors(mode: UserThemeMode) {
        activity?.let {
            var backgroundColor = it.getAttrColor(materialRes.attr.colorSurfaceContainer)
            var statusBarColor = it.getAttrColor(materialRes.attr.colorSurfaceContainer)
            if (mode == UserThemeMode.AMOLED_DARK) {
                statusBarColor = Colors.BLACK
                backgroundColor = Colors.BLACK
            }

            binding.coordinatorLayout.setBackgroundColor(backgroundColor)
            binding.collapsingToolbar.setContentScrimColor(backgroundColor)
            binding.collapsingToolbar.setStatusBarScrimColor(statusBarColor)
            binding.collapsingToolbar.setBackgroundColor(statusBarColor)
            binding.appBar.setLiftOnScrollColor(ColorStateList.valueOf(statusBarColor))
            if (binding.appBar.background is MaterialShapeDrawable) {
                val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                binding.appBar.setBackgroundColor(statusBarColor)
            }
        }

        // Update preference background tint
        if (listView.adapter.isAdapterOfTypeOrConcatContains(SettingsPreferenceGroupAdapter::class.java)) {
            listView.adapter?.notifyDataSetChanged()
        }
    }

    private fun Preference.tintIcons(@ColorInt color: Int) {
        if (this is PreferenceGroup) {
            for (i in 0 until preferenceCount) {
                getPreference(i).tintIcons(color)
            }
        } else {
            val icon: Drawable? = icon
            icon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}