package com.thewizrd.simpleweather.preferences

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.platform.MaterialFadeThrough
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
    val toolbar: MaterialToolbar
        get() = binding.toolbar

    override fun createSnackManager(activity: Activity): SnackbarManager? {
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
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
        val root: ViewGroup = binding.coordinatorLayout
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        val lp = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.behavior = ScrollingViewBehavior()
        root.addView(inflatedView, lp)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.toolbar.setTitle(titleResId)
        binding.appBar.liftOnScrollTargetViewId = listView.id
    }

    override fun updateWindowColors() {
        super.updateWindowColors()
        updateWindowColors(settingsManager.getUserThemeMode())
    }

    protected fun updateWindowColors(mode: UserThemeMode) {
        activity?.let {
            var backgroundColor = it.getAttrColor(android.R.attr.colorBackground)
            var statusBarColor = it.getAttrColor(R.attr.colorSurface)
            if (mode == UserThemeMode.AMOLED_DARK) {
                statusBarColor = Colors.BLACK
                backgroundColor = statusBarColor
            }

            binding.coordinatorLayout.setBackgroundColor(backgroundColor)
            if (binding.appBar.background is MaterialShapeDrawable) {
                val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                binding.appBar.setBackgroundColor(statusBarColor)
            }
        }
    }
}