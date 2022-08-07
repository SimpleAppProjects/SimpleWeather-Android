package com.thewizrd.simpleweather.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentToolbarLayoutBinding

abstract class ToolbarFragment : WindowColorFragment() {
    // Views
    private lateinit var binding: FragmentToolbarLayoutBinding

    val appBarLayout: AppBarLayout
        get() = binding.appBar
    val rootView: CoordinatorLayout
        get() = binding.rootView
    val toolbar: MaterialToolbar
        get() = binding.toolbar

    @get:StringRes
    protected abstract val titleResId: Int

    @get:IdRes
    protected open val scrollTargetViewId: Int
        get() = View.NO_ID

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentToolbarLayoutBinding.inflate(inflater, container, false)
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

        // Toolbar
        binding.toolbar.setTitle(titleResId)

        return binding.root
    }

    protected fun setNavigationIconVisible(visible: Boolean) {
        if (visible) {
            toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        } else {
            toolbar.navigationIcon = null
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.liftOnScrollTargetViewId = scrollTargetViewId
    }

    @CallSuper
    override fun updateWindowColors() {
        context?.let { ctx ->
            var backgroundColor = ctx.getAttrColor(android.R.attr.colorBackground)
            var statusBarColor = ctx.getAttrColor(R.attr.colorSurface)
            if (settingsManager.getUserThemeMode() === UserThemeMode.AMOLED_DARK) {
                statusBarColor = Colors.BLACK
                backgroundColor = statusBarColor
            }

            binding.rootView.setBackgroundColor(backgroundColor)
            if (binding.appBar.background is MaterialShapeDrawable) {
                val materialShapeDrawable = binding.appBar.background as MaterialShapeDrawable
                materialShapeDrawable.fillColor = ColorStateList.valueOf(statusBarColor)
            } else {
                binding.appBar.setBackgroundColor(statusBarColor)
            }
        }
    }
}