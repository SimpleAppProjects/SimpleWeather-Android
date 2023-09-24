package com.thewizrd.simpleweather.preferences

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.common.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.PreferenceListHeaderAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding
import com.thewizrd.simpleweather.helpers.CustomScrollingLayoutCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class SwipeDismissPreferenceFragment : PreferenceFragmentCompat() {
    companion object {
        private const val DIALOG_FRAGMENT_TAG =
            "com.thewizrd.simpleweather.preferences.SwipeDismissPreferenceFragment.DIALOG"
    }

    @IntDef(Toast.LENGTH_SHORT, Toast.LENGTH_LONG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ToastDuration

    private lateinit var binding: ActivitySettingsBinding
    private var swipeCallback: SwipeDismissFrameLayout.Callback? = null

    @get:StringRes
    protected abstract val titleResId: Int

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsBinding.inflate(inflater, container, false)
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        // Add padding for time text and scroll
        inflatedView.findViewById<RecyclerView>(R.id.recycler_view)?.let {
            it.updatePadding(top = inflater.context.dpToPx(12f).toInt())

            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.timeText.apply {
                        translationY = -recyclerView.computeVerticalScrollOffset().toFloat()
                    }
                }
            })
        }

        binding.swipeLayout.addView(inflatedView)
        binding.swipeLayout.isSwipeable = true
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                layout.visibility = View.GONE
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
        binding.swipeLayout.addCallback(swipeCallback)
        binding.swipeLayout.requestFocus()

        return binding.swipeLayout
    }

    override fun onResume() {
        super.onResume()
        binding.swipeLayout.requestFocus()
    }

    override fun onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }

    fun showToast(@StringRes resId: Int, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (isVisible) {
                Toast.makeText(requireContext(), resId, duration).show()
            }
        }
    }

    fun showToast(message: CharSequence?, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (isVisible) {
                Toast.makeText(requireContext(), message, duration).show()
            }
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param block the coroutine code which will be invoked in the context of the viewLifeCycleOwner lifecycle scope.
     */
    fun runWithView(context: CoroutineContext = EmptyCoroutineContext,
                    block: suspend CoroutineScope.() -> Unit
    ) {
        runCatching {
            viewLifecycleOwner.lifecycleScope
        }.onFailure {
            // no-op
        }.onSuccess {
            it.launch(context = context, block = block)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            inflater.inflate(R.layout.preference_recyclerview, parent, false) as RecyclerView

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(
            SpacerItemDecoration(
                recyclerView.context.dpToPx(16f).toInt(),
                recyclerView.context.dpToPx(4f).toInt()
            )
        )
        recyclerView.layoutManager = onCreateLayoutManager()
        recyclerView.setAccessibilityDelegateCompat(
            PreferenceRecyclerViewAccessibilityDelegate(
                recyclerView
            )
        )

        return recyclerView
    }

    override fun onCreateLayoutManager(): RecyclerView.LayoutManager {
        return WearableLinearLayoutManager(context, CustomScrollingLayoutCallback())
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return ConcatAdapter(
            PreferenceListHeaderAdapter(requireContext().getString(titleResId)),
            super.onCreateAdapter(preferenceScreen),
            SpacerAdapter(requireContext().dpToPx(48f).toInt())
        )
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        // check if dialog is already showing
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }

        when (preference) {
            is WearEditTextPreference -> {
                val f = WearEditTextPreferenceDialogFragment.newInstance(preference.key)
                f.setTargetFragment(this, 0)
                f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
            }
            is WearListPreference -> {
                val f = WearListPreferenceDialogFragment.newInstance(preference.key)
                f.setTargetFragment(this, 0)
                f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
            }
            else -> {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }
}