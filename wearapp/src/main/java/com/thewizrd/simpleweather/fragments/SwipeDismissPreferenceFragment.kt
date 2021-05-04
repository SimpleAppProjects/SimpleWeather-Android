package com.thewizrd.simpleweather.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.wear.widget.SwipeDismissFrameLayout
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class SwipeDismissPreferenceFragment : LifecyclePreferenceFragment() {
    @IntDef(Toast.LENGTH_SHORT, Toast.LENGTH_LONG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ToastDuration

    var parentActivity: Activity? = null
        private set
    protected val settingsManager: SettingsManager = App.instance.settingsManager

    private lateinit var binding: ActivitySettingsBinding
    private var swipeCallback: SwipeDismissFrameLayout.Callback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as Activity
    }

    override fun onDetach() {
        parentActivity = null
        super.onDetach()
    }

    override fun onDestroy() {
        parentActivity = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreatePreferences(savedInstanceState)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        binding = ActivitySettingsBinding.inflate(inflater, container, false)
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.swipeLayout.addView(inflatedView)
        binding.swipeLayout.isSwipeable = true
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentActivity?.onBackPressed()
            }
        }
        binding.swipeLayout.addCallback(swipeCallback)
        binding.swipeLayout.requestFocus()

        return binding.swipeLayout
    }

    override fun onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }

    abstract fun onCreatePreferences(savedInstanceState: Bundle?)

    fun showToast(@StringRes resId: Int, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (parentActivity != null && isVisible) {
                Toast.makeText(parentActivity, resId, duration).show()
            }
        }
    }

    fun showToast(message: CharSequence?, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (parentActivity != null && isVisible) {
                Toast.makeText(parentActivity, message, duration).show()
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
}